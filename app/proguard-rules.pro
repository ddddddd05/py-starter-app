# R8（代码压缩）的保留规则。
#
# Chaquopy、Room、Compose、AndroidX 都自带了各自需要的规则（consumer rules），这里只补本项目自己的。
# Python 层只被 Java 调用（Java → Python），Python 不反射调用我们的 Java 类，所以不需要额外保留。

# 崩溃日志里保留文件名和行号，方便排查问题
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
