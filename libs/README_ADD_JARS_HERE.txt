# 本地依赖 jar 目录（可选）
# 构建通过 `implementation fileTree(dir:'libs', include:'*.jar')` 加载此目录下的所有 jar。
# 本模组已不依赖任何 UI 库（HUD 用原版 GuiGraphics 渲染）；Spell Engine 通过反射访问、运行期由信雅互联注入，
# 因此通常无需在此放置任何 jar。如需引入其它编译期依赖，把对应 jar 放这里即可。
