#include "welcomedialog.h"
#include <QNetworkAccessManager>
#include <QNetworkRequest>
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonArray>
#include <QEventLoop>
#include <QFile>
#include <QDir>
#include <QDebug>
#include <QDesktopServices>
#include <QUrl>
#include <QMessageBox>

WelcomeDialog::WelcomeDialog(const QString &username, QWidget *parent)
    : QDialog(parent), m_username(username)
{
    setWindowTitle("欢迎");
    setMinimumSize(500, 250);

    QVBoxLayout *layout = new QVBoxLayout(this);

    // 欢迎信息
    QLabel *welcomeLabel = new QLabel(QString("欢迎，%1！").arg(username), this);
    layout->addWidget(welcomeLabel);

    // 从后端获取 Keys
    QJsonArray keys = fetchKeysFromServer(username);

    if (keys.isEmpty()) {
        // Keys 为空，显示提示信息和链接
        m_labelInfo = new QLabel("您还没有 API Keys，请先注册并登录。", this);
        layout->addWidget(m_labelInfo);

        QLabel *linkLabel = new QLabel(
            "<a href='http://ai.ainetlab.net/login'>请点击这里登录网站注册</a>",
            this
        );
        linkLabel->setOpenExternalLinks(true);
        layout->addWidget(linkLabel);
    } else {
        // 有 Keys，自动更新配置文件
        m_labelInfo = new QLabel("欢迎使用 AI Token！", this);
        layout->addWidget(m_labelInfo);

        // 显示获取到的 Keys 数量
        QLabel *keysCountLabel = new QLabel(
            QString("已成功获取 %1 个 API Key").arg(keys.size()),
            this
        );
        layout->addWidget(keysCountLabel);

        // 更新配置文件
        if (updateSettingsJson(keys)) {
            QMessageBox::information(this, "成功", "配置文件已自动更新。");
        } else {
            QMessageBox::warning(this, "警告", "更新配置文件失败，请检查权限。");
        }
    }

    // 关闭按钮
    m_closeButton = new QPushButton("关闭", this);
    layout->addWidget(m_closeButton);
    connect(m_closeButton, &QPushButton::clicked, this, &WelcomeDialog::close);
}

QJsonArray WelcomeDialog::fetchKeysFromServer(const QString &username)
{
    QNetworkAccessManager manager;
    QString urlStr = QString("http://localhost:8082/api/keys/qt/%1").arg(username);
    QNetworkRequest request(QUrl(urlStr));

    QNetworkReply* reply = manager.get(request);

    QEventLoop loop;
    QObject::connect(reply, &QNetworkReply::finished, &loop, &QEventLoop::quit);
    loop.exec();

    QJsonDocument doc = QJsonDocument::fromJson(reply->readAll());
    reply->deleteLater();

    QJsonObject response = doc.object();

    // 从响应中提取 data 数组
    if (response["code"].toInt() == 0) {
        return response["data"].toArray();
    }

    qWarning() << "获取 Keys 失败:" << response["msg"].toString();
    return QJsonArray();
}

bool WelcomeDialog::updateSettingsJson(const QJsonArray &keys)
{
    if (!ensureSettingsDirExists()) {
        qWarning() << "无法创建配置文件所在目录";
        return false;
    }

    QString filePath = getSettingsFilePath();
    qDebug() << "正在更新配置文件：" << filePath;

    QFile file(filePath);
    QJsonDocument doc;
    QJsonObject root;

    if (file.exists()) {
        if (!file.open(QIODevice::ReadOnly | QIODevice::Text)) {
            qWarning() << "无法打开配置文件进行读取：" << filePath;
            return false;
        }
        QByteArray data = file.readAll();
        file.close();
        doc = QJsonDocument::fromJson(data);
        if (!doc.isNull() && doc.isObject()) {
            root = doc.object();
        } else {
            QFile::copy(filePath, filePath + ".bak");
            qWarning() << "配置文件格式错误，已备份为" << filePath + ".bak，将重新创建。";
            root = QJsonObject();
        }
    }

    // 更新 apiKeys 字段
    root["apiKeys"] = keys;

    if (!file.open(QIODevice::WriteOnly | QIODevice::Text)) {
        qWarning() << "无法打开配置文件进行写入：" << filePath;
        return false;
    }
    doc.setObject(root);
    file.write(doc.toJson(QJsonDocument::Indented));
    file.close();

    qDebug() << "配置文件已成功更新：" << filePath;
    return true;
}

bool WelcomeDialog::ensureSettingsDirExists()
{
    QString filePath = getSettingsFilePath();
    QFileInfo fileInfo(filePath);
    QDir dir = fileInfo.absoluteDir();
    if (!dir.exists()) {
        if (!dir.mkpath(".")) {
            qWarning() << "无法创建目录：" << dir.absolutePath();
            return false;
        }
        qDebug() << "已创建目录：" << dir.absolutePath();
    }
    return true;
}

QString WelcomeDialog::getSettingsFilePath() const
{
    return QDir::homePath() + "/.claude/settings.json";
}