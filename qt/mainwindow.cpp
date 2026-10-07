#include "mainwindow.h"
#include <QNetworkAccessManager>
#include <QNetworkRequest>
#include <QJsonDocument>
#include <QJsonObject>
#include <QEventLoop>
#include <QMessageBox>
#include "./ui_mainwindow.h"
#include "WelcomeDialog.h"
MainWindow::MainWindow(QWidget *parent)
    : QMainWindow(parent)
    , ui(new Ui::MainWindow)
{
    ui->setupUi(this);
    setWindowTitle("登录注册");   // 设置窗口标题
}

MainWindow::~MainWindow()
{
    delete ui;
}

void MainWindow::on_login_clicked()
{
    QString username = ui->lineEdit_username->text().trimmed();
    QString password = ui->lineEdit_password->text();

    if (username.isEmpty() || password.isEmpty()) {
        QMessageBox::warning(this, "警告", "用户名或密码不能为空！");
        return;
    }

    // 调用后端登录 API
    QNetworkAccessManager manager;
    QNetworkRequest request(QUrl("http://localhost:8082/api/auth/login"));
    request.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");

    QJsonObject body;
    body["username"] = username;
    body["password"] = password;

    QNetworkReply* reply = manager.post(request, QJsonDocument(body).toJson());

    QEventLoop loop;
    QObject::connect(reply, &QNetworkReply::finished, &loop, &QEventLoop::quit);
    loop.exec();

    QJsonDocument doc = QJsonDocument::fromJson(reply->readAll());
    QJsonObject response = doc.object();
    reply->deleteLater();

    // 检查登录结果
    if (response["code"].toInt() == 0) {
        // 登录成功，跳转到欢迎窗口，传入用户名
        WelcomeDialog *welcome = new WelcomeDialog(username, this);
        connect(welcome, &WelcomeDialog::finished, this, &MainWindow::show);
        welcome->show();
        this->hide();
    } else {
        QString errorMsg = response["msg"].toString();
        if (errorMsg.isEmpty()) {
            errorMsg = "登录失败，请检查用户名或密码！";
        }
        QMessageBox::warning(this, "登录失败", errorMsg);
    }
}

