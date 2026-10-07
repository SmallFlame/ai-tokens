#ifndef WELCOMEDIALOG_H
#define WELCOMEDIALOG_H

#include <QDialog>
#include <QLineEdit>
#include <QLabel>
#include <QPushButton>
#include <QVBoxLayout>
#include <QJsonArray>

class WelcomeDialog : public QDialog
{
    Q_OBJECT
public:
    explicit WelcomeDialog(const QString &username, QWidget *parent = nullptr);
private:
    QString m_username;
    QLabel *m_labelInfo;
    QPushButton *m_closeButton;

    QJsonArray fetchKeysFromServer(const QString &username);
    bool updateSettingsJson(const QJsonArray &keys);
    QString getSettingsFilePath() const;
    bool ensureSettingsDirExists();
};

#endif // WELCOMEDIALOG_H