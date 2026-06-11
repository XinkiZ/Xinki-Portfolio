#!/bin/bash
# Xinki Portfolio — 一键部署脚本
# 支持: CentOS 7.9 / Alibaba Cloud Linux / Ubuntu
# 用法: chmod +x deploy.sh && ./deploy.sh

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "========================================"
echo "  Xinki Portfolio Docker Deploy"
echo "========================================"

# 1. 检查 .env
if [ ! -f .env ]; then
    echo -e "${YELLOW}[!] .env 文件不存在，正在从 .env.example 创建...${NC}"
    cp .env.example .env
    echo -e "${RED}[!] 请编辑 .env 填入你的 API Key 和密码，然后重新运行此脚本${NC}"
    exit 1
fi

# 加载 .env
set -a
source .env
set +a

# 2. 检查并安装 Docker（CentOS 7 特殊处理）
if ! command -v docker &> /dev/null; then
    echo -e "${YELLOW}[*] Docker 未安装，正在安装...${NC}"
    if [ -f /etc/centos-release ]; then
        # CentOS 7
        sudo yum install -y yum-utils
        sudo yum-config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
        sudo yum install -y docker-ce docker-ce-cli containerd.io
        sudo systemctl start docker
        sudo systemctl enable docker
    elif [ -f /etc/alinux-release ] || grep -qi "Alibaba" /etc/os-release 2>/dev/null; then
        # Alibaba Cloud Linux
        sudo yum install -y docker
        sudo systemctl start docker
        sudo systemctl enable docker
    else
        # Ubuntu / Debian
        sudo apt-get update
        sudo apt-get install -y docker.io
        sudo systemctl start docker
        sudo systemctl enable docker
    fi
    echo -e "${GREEN}[+] Docker 安装完成${NC}"
fi

# 3. 开放防火墙端口（阿里云 ECS 需在安全组放行 80/443）
echo -e "${YELLOW}[*] 请确保阿里云 ECS 安全组已放行 80 和 443 端口${NC}"

# 4. 检查 docker compose
if docker compose version &> /dev/null; then
    COMPOSE="docker compose"
elif command -v docker-compose &> /dev/null; then
    COMPOSE="docker-compose"
else
    echo -e "${YELLOW}[*] docker-compose 未安装，正在安装...${NC}"
    sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    COMPOSE="docker-compose"
    echo -e "${GREEN}[+] docker-compose 安装完成${NC}"
fi

# 5. 创建 SSL 目录
mkdir -p deploy/nginx/ssl

# 6. 检查 SSL 证书
if [ ! -f deploy/nginx/ssl/xinki.xyz.pem ] || [ ! -f deploy/nginx/ssl/xinki.xyz.key ]; then
    echo -e "${YELLOW}[!] SSL 证书未找到（deploy/nginx/ssl/xinki.xyz.pem + .key）${NC}"
    echo -e "${YELLOW}[!] 请从阿里云 SSL 证书控制台下载并放置证书文件${NC}"
    echo -e "${YELLOW}[!] 在此之前将以 HTTP 模式启动（可手动修改 nginx.conf 临时去掉 301 重定向）${NC}"
fi

# 7. 确保当前用户有 Docker 权限
if ! docker ps &> /dev/null; then
    echo -e "${YELLOW}[*] 将当前用户加入 docker 组...${NC}"
    sudo usermod -aG docker $USER
    echo -e "${RED}[!] 请退出重新登录使 docker 组生效，或使用 sudo 运行此脚本${NC}"
    echo -e "${RED}[!] 命令: newgrp docker && ./deploy.sh${NC}"
    exit 1
fi

# 8. 拉取基础镜像
echo -e "${GREEN}[*] 拉取基础镜像...${NC}"
docker pull mysql:8.0
docker pull redis:7-alpine
docker pull nginx:alpine
docker pull maven:3.9-eclipse-temurin-17-alpine
docker pull eclipse-temurin:17-jre-alpine
docker pull node:18-alpine
echo -e "${GREEN}[+] 基础镜像拉取完成${NC}"

# 9. 构建并启动
echo -e "${GREEN}[*] 构建并启动所有服务（首次构建约 5-10 分钟）...${NC}"
$COMPOSE up -d --build

# 10. 等待健康检查
echo -e "${GREEN}[*] 等待服务就绪...${NC}"
sleep 15

# 11. 状态检查
echo ""
echo "========================================"
echo -e "${GREEN}  部署完成！${NC}"
echo "========================================"
echo "  访问地址: https://${DOMAIN:-www.xinki.xyz}"
echo "            http://<服务器IP>"
echo ""
echo "  管理后台: https://${DOMAIN:-www.xinki.xyz}/admin"
echo "  默认账号: admin / admin123（首次登录后请立即修改）"
echo ""
echo "  服务状态:"
$COMPOSE ps
echo ""
echo "  查看日志: $COMPOSE logs -f"
echo "  重启服务: $COMPOSE restart"
echo "  停止服务: $COMPOSE down"
echo "  更新部署: git pull && $COMPOSE up -d --build"
echo "========================================"