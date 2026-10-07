#include "mainwindow.h"

#include <QApplication>
#include "registerdialog.h"   // 新增

int main(int argc, char *argv[])
{
    QApplication a(argc, argv);
    MainWindow w;
    w.show();
    return QCoreApplication::exec();
}
