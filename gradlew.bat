@rem FocusLock Gradle launcher
@echo off
setlocal
set APP_HOME=%~dp0
set WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
if not exist "%WRAPPER_JAR%" (
  echo Missing gradle/wrapper/gradle-wrapper.jar. Open this project in Android Studio once or add the standard Gradle wrapper jar.
  exit /b 1
)
if not exist "%JAVA_HOME%\bin\java.exe" (
  if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
  )
)
if exist "%JAVA_HOME%\bin\java.exe" (
  set "JAVACMD=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVACMD=java.exe"
)
"%JAVACMD%" -jar "%WRAPPER_JAR%" %*
