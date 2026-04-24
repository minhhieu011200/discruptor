@echo off
cd /d d:\EXIM\code\discruptor

set JAVA_HOME=C:\Program Files\Java\jdk-21

"%JAVA_HOME%\bin\java.exe" ^
-Dchronicle.map.disable.compiler=true ^
--enable-native-access=ALL-UNNAMED ^
--add-opens java.base/java.lang=ALL-UNNAMED ^
--add-opens java.base/java.lang.reflect=ALL-UNNAMED ^
--add-opens java.base/java.lang.invoke=ALL-UNNAMED ^
--add-opens java.base/jdk.internal.misc=ALL-UNNAMED ^
--add-opens java.base/java.nio=ALL-UNNAMED ^
--add-opens jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED ^
--add-opens jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED ^
--add-opens jdk.compiler/com.sun.tools.javac=ALL-UNNAMED ^
--add-opens jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED ^
--add-opens jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED ^
--add-opens jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED ^
--add-opens jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED ^
--add-opens jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED ^
--add-opens jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED ^
--add-opens jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED ^
--add-opens jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED ^
--add-opens java.base/sun.nio.ch=ALL-UNNAMED ^
@C:\Users\minhh\AppData\Local\Temp\cp_b5sej4wkqwz1udzgf8ea8quha.argfile ^
com.example.demo.DemoApplication

pause