# Android 国际化多语言开发助手(Android DevKit)

## 功能

- 支持对送翻多语言进行一键同步操作；
- 支持对 Android res 下 string 的重复值静态检测及建议通过 @string/[duplicate] 替换；
- 支持配置常用网站便捷访问；

## 默认快捷键(可在 Setting 中修改) 

- 多语言同步：control alt 0
- 项目常用网站：control alt 1
- 安卓字符串资源值检测：control alt 2

## 操作演示

送翻前多语言重复性静态检查及纠正建议提示操作，譬如一个 string_i18n.xml 字符串文件中存在相同值的字符串，这对送翻来说是浪费人力财力。

<div><img src=".images/res-repeat-inspection.gif"></div>

送翻前多语言同步及格式化，方便给翻译公司提供 xml 文件，然后翻译后直接回来入库，避免出现意外。

<div><img src=".images/res-i18n-pre-trans-format.gif"></div>

项目相关常用网站 Android Studio 应用内快捷访问，提升开发效率。

<div><img src=".images/fast-net-work.gif"></div>

## 注意事项

多语言源文件书写格式必须是标准 XML，一个完整的 `<string>xxxxx<string/>` 中必须不准换行，同步原理为按行同步，可以参照如下格式。

```xml
<resources>
    <string name="app_name">My Application</string>
    <string name="app_name1">@string/app_name</string>
    
    <!-- xxxxxx -->
    <array name="test">
        <item>xxix</item>
        <item>@string/app_name</item>
    </array>
</resources>
```