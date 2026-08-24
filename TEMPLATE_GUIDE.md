# Kuikly 模板项目使用指南

## 模板仓库

https://github.com/wookkkkk/kuiklyTemplete

## 创建新项目

### 1. 从模板克隆

```bash
git clone https://github.com/wookkkkk/kuiklyTemplete.git <新项目名>
cd <新项目名>
```

### 2. 删除旧 Git 历史，重新初始化

```bash
rd /s /q .git
git init -b main
git add -A
git commit -m "Initial commit"
```

### 3. 关联新的 GitHub 仓库

在 GitHub 上创建一个空仓库（不要勾选 README / .gitignore / license），然后：

```bash
git remote add origin https://github.com/<你的用户名>/<新仓库名>.git
git push -u origin main
```

### 4. 修改项目标识（可选）

- `buildSrc/src/main/java/KotlinBuildVar.kt` — 应用名/版本号
- `androidApp/build.gradle.kts` — Android 包名
- `iosApp/` — iOS Bundle Identifier
- `ohosApp/` — HarmonyOS 包名

---

## 快速创建（一行命令）

```bash
git clone https://github.com/wookkkkk/kuiklyTemplete.git my-new-app && cd my-new-app && rd /s /q .git && git init && git add -A && git commit -m "Initial commit"
```

然后去 GitHub 创建空仓库，再执行：

```bash
git remote add origin https://github.com/<你的用户名>/<新仓库名>.git
git push -u origin main
```
