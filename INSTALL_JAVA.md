# Installing Java to Build the Plugin

The plugin requires Java 11 or higher to build. Here's how to install it:

## On RHEL/CentOS/Fedora (Your System)

```bash
# Install Java 11 (OpenJDK)
sudo yum install java-11-openjdk-devel

# Verify installation
java -version
```

## On Ubuntu/Debian

```bash
sudo apt update
sudo apt install openjdk-11-jdk

# Verify installation
java -version
```

## On macOS

```bash
# Using Homebrew
brew install openjdk@11

# Add to PATH
echo 'export PATH="/usr/local/opt/openjdk@11/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Verify installation
java -version
```

## After Installing Java

Once Java is installed, you can build the plugin:

```bash
cd kiro_projects/dmm-flipper-plugin
./gradlew build
```

The JAR file will be created at: `build/libs/dmm-flipper-plugin-1.0.0.jar`

## Alternative: Use Pre-built JAR

If you can't install Java on this system, you can:

1. Copy the project to a machine with Java installed
2. Build it there
3. Copy the JAR back

Or:

1. Use GitHub Actions or another CI service to build it
2. Download the built JAR

## Checking Java Version

After installation, verify you have Java 11+:

```bash
java -version
# Should show: openjdk version "11.x.x" or higher
```

## Troubleshooting

**"JAVA_HOME is not set"**
```bash
# Find Java installation
which java

# Set JAVA_HOME (add to ~/.bashrc for persistence)
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

**"Permission denied" when running gradlew**
```bash
chmod +x gradlew
```
