#ifndef REGISTERDIALOG_H
#define REGISTERDIALOG_H

#include <QDialog>
#include <QLineEdit>
#include <QLabel>
#include <QPushButton>
#include <QVBoxLayout>
#include <QHBoxLayout>

class RegisterDialog : public QDialog
{
    Q_OBJECT
public:
    explicit RegisterDialog(QWidget *parent = nullptr);
private slots:
    void onRegisterClicked();
private:
    QLineEdit *m_usernameEdit;
    QLineEdit *m_passwordEdit;
    QLineEdit *m_confirmEdit;
    QLabel *m_errorLabel;
    QPushButton *m_registerButton;
    QPushButton *m_cancelButton;

    bool checkUsernameExists(const QString &username);
    bool saveUser(const QString &username, const QString &password);
};

#endif // REGISTERDIALOG_H