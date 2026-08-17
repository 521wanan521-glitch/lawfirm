# 代码推送说明

> 代码已在本地完成 Git 提交（commit `6ad0972`，164 个文件），并关联远程仓库：
> `https://github.com/521wanan521-glitch/lawfirm.git`

推送需要 GitHub 认证，二选一完成即可。

---

## 方式一：提供访问令牌，由我代推送（推荐）

1. 打开 GitHub 令牌页面：https://github.com/settings/tokens
2. 点击 **Generate new token (classic)**
3. 勾选 **repo** 权限（完整读写仓库）
4. 点击生成，复制 `ghp_` 开头的令牌
5. 把令牌发给我（对话中回复即可），我用它完成推送

> 安全提示：令牌会出现在推送命令中，**推送完成后你可立即在 GitHub 上吊销（Revoke）该令牌**。

---

## 方式二：在你自己电脑上推送

如果你有安装了 Git 的电脑，按下面步骤操作（把 `<TOKEN>` 换成你的令牌）：

```bash
# 1. 克隆空仓库到本地
git clone https://github.com/521wanan521-glitch/lawfirm.git
cd lawfirm

# 2. 把本项目的全部代码复制到该目录（不要复制 .git、node_modules、target、dist、data 目录）

# 3. 提交并推送
git add .
git commit -m "feat: 律所数字化办公系统 - 后端 + 前端 + Docker 部署 + 文档"
git push -u origin main
```

首次 push 时 Git 会提示输入账号，用户名填你的 GitHub 用户名，密码填访问令牌（不是登录密码）。

---

## 附：本次本地已完成的配置

- 本地仓库已初始化，默认分支 `main`
- 远程已添加：`origin -> https://github.com/521wanan521-glitch/lawfirm.git`
- `.gitignore` 已排除 `node_modules`、`target`、`dist`、`data`、`.tools` 等构建/本地产物
