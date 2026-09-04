# Raspberry Pi production preparation

This directory contains templates only. Never commit the real environment file.

## Build on the development computer

```powershell
.\mvnw.cmd clean package
```

The deployable JAR is created under `target/`. Rename the Spring Boot JAR to
`hockey-server.jar` when copying it to the Raspberry Pi.

## Raspberry Pi directories and service account

```bash
sudo useradd --system --home /opt/hockey-server --shell /usr/sbin/nologin hockey-server
sudo install -d -o hockey-server -g hockey-server -m 0750 /opt/hockey-server
sudo install -d -o root -g hockey-server -m 0750 /etc/hockey-server
```

Copy the JAR to `/opt/hockey-server/hockey-server.jar`, copy
`hockey-server.env.example` to `/etc/hockey-server/hockey-server.env`, then
replace all placeholder values. The real environment file must be readable by
root and the service group only:

```bash
sudo chown root:hockey-server /etc/hockey-server/hockey-server.env
sudo chmod 0640 /etc/hockey-server/hockey-server.env
sudo chown hockey-server:hockey-server /opt/hockey-server/hockey-server.jar
sudo chmod 0750 /opt/hockey-server/hockey-server.jar
```

Generate the JWT secret on the Raspberry Pi:

```bash
openssl rand -base64 32
```

## Install the systemd unit

```bash
sudo cp deploy/hockey-server.service /etc/systemd/system/hockey-server.service
sudo systemctl daemon-reload
sudo systemctl enable hockey-server.service
sudo systemctl start hockey-server.service
sudo systemctl status hockey-server.service --no-pager
```

View logs with:

```bash
sudo journalctl -u hockey-server.service -n 100 --no-pager
sudo journalctl -u hockey-server.service -f
```

The production profile binds the application to `127.0.0.1`, so it is not
directly exposed to the internet. Nginx will provide HTTPS and WSS in the next
deployment step.

