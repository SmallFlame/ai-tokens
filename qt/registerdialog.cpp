#include "registerdialog.h"
#include <QFile>
#include <QTextStream>
#include <QMessageBox>
#include <QCoreApplication>

RegisterDialog::RegisterDialog(QWidget *parent)
    : QDialog(parent)
{
    setWindowTitle("注册新账户");
    setMinimumSize(300, 200);

    QVBoxLayout *mainLayout = new QVBoxLayout(this);

    // 用户名
    QHBoxLayout *userLayout = new QHBoxLayout();
    QLabel *userLabel = new QLabel("用户名：", this);
    m_usernameEdit = new QLineEdit(this);
    userLayout->addWidget(userLabel);
    userLayout->addWidget(m_usernameEdit);
    mainLayout->addLayout(userLayout);

    // 密码
    QHBoxLayout *pwdLayout = new QHBoxLayout();
    QLabel *pwdLabel = new QLabel("密码：", this);
    m_passwordEdit = new QLineEdit(this);
    m_passwordEdit->setEchoMode(QLineEdit::Password);
    pwdLayout->addWidget(pwdLabel);
    pwdLayout->addWidget(m_passwordEdit);
    mainLayout->addLayout(pwdLayout);

    // 确认密码
    QHBoxLayout *confirmLayout = new QHBoxLayout();
    QLabel *confirmLabel = new QLabel("确认密码：", this);
    m_confirmEdit = new QLineEdit(this);
    m_confirmEdit->setEchoMode(QLineEdit::Password);
    confirmLayout->addWidget(confirmLabel);
    confirmLayout->addWidget(m_confirmEdit);
    mainLayout->addLayout(confirmLayout);

    // 错误提示标签
    m_errorLabel = new QLabel(this);
    m_errorLabel->setStyleSheet("color: red;");
    m_errorLabel->setVisible(false);
    mainLayout->addWidget(m_errorLabel);

    // 按钮
    QHBoxLayout *btnLayout = new QHBoxLayout();
    m_registerButton = new QPushButton("注册", this);
    m_cancelButton = new QPushButton("取消", this);
    btnLayout->addWidget(m_registerButton);
    btnLayout->addWidget(m_cancelButton);
    mainLayout->addLayout(btnLayout);

    connect(m_registerButton, &QPushButton::clicked, this, &RegisterDialog::onRegisterClicked);
    connect(m_cancelButton, &QPushButton::clicked, this, &RegisterDialog::reject);
}

void RegisterDialog::onRegisterClicked()
{
    QString username = m_usernameEdit->text().trimmed();
    QString password = m_passwordEdit->text();
    QString confirm = m_confirmEdit->text();

    // 验证输入
    if (username.isEmpty()) {
        m_errorLabel->setText("用户名不能为空！");
        m_errorLabel->setVisible(true);
        return;
    }
    if (password.isEmpty()) {
        m_errorLabel->setText("密码不能为空！");
        m_errorLabel->setVisible(true);
        return;
    }
    if (password != confirm) {
        m_errorLabel->setText("两次输入的密码不一致！");
        m_errorLabel->setVisible(true);
        return;
    }

    // 检查用户名是否已存在
    if (checkUsernameExists(username)) {
        m_errorLabel->setText("用户名已存在！");
        m_errorLabel->setVisible(true);
        return;
    }

    // 保存用户
    if (saveUser(username, password)) {
        QMessageBox::information(this, "注册成功", "账户创建成功！请使用新账户登录。");
        accept();
    } else {
        QMessageBox::critical(this, "注册失败", "无法保存用户数据，请检查文件权限。");
    }
}

bool RegisterDialog::checkUsernameExists(const QString &username)
{
    QString filePath = QCoreApplication::applicationDirPath() + "/users.txt";
    QFile file(filePath);
    if (!file.exists()) {
        return false; // 文件不存在，用户名肯定不存在
    }
    if (!file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        return false; // 无法读取，保守返回 false
    }
    QTextStream in(&file);
    while (!in.atEnd()) {
        QString line = in.readLine();
        if (line.isEmpty()) continue;
        QStringList parts = line.split(':');
        if (parts.size() >= 1 && parts[0].trimmed() == username) {
            file.close();
            return true;
        }
    }
    file.close();
    return false;
}

bool RegisterDialog::saveUser(const QString &username, const QString &password)
{
    QString filePath = QCoreApplication::applicationDirPath() + "/users.txt";
    QFile file(filePath);
    // 以追加模式打开（如果文件不存在会自动创建）
    if (!file.open(QIODevice::Append | QIODevice::Text)) {
        return false;
    }
    QTextStream out(&file);
    // 格式：用户名:密码:key（key 为空）
    out << username << ":" << password << ":\n";
    file.close();
    return true;
}