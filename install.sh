# pick latest version number from GitHub releases if needed
VERSION=1.5.1
ARCH=linux_amd64   # use linux_arm64 for ARM

curl -LO https://github.com/charmbracelet/glow/releases/download/v$VERSION/glow_${VERSION}_${ARCH}.tar.gz
tar -xzf glow_${VERSION}_${ARCH}.tar.gz
sudo mv glow /usr/local/bin/