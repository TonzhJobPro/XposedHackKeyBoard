# KTS
Kika Test Suite

### 工程结构

> **该工程运行基于Xposed的框架，属于Xposed一个模块**

> Xposed开源项目 [https://github.com/rovo89](https://github.com/rovo89)

项目：

|---- `app`: Xposed模块 -- 用于通过Xposed框架接口获取需要的app信息

|---- `hackbasicmodule`: android模块 -- 输入法metrics相关流程具体实现，该模块可以直接移植到kika输入法中使用

|---- `javaprocess`: java模块 -- 处理生成输入相关的配置文件以及发送测试命令



### 基本思路

1. 通过Xposed获取到主键盘、以及候选词相关对象
2. 启动Handler接受adb发送的测试命令
3. 通过主键盘View发送post motionEvent模拟用户输入
4. 通过1获取的候选词对象处理每次输入的候选结果
5. 将候选结果写入文本

### 测试配置信息制作
信息类似于swift：
```json
{"packageName":"com.touchtype.swiftkey","inputmethodServiceClazz":"com.touchtype.KeyboardService","mainKeyboardViewClazz":"com.touchtype.keyboard.view.o","candidateViewClazz":"com.touchtype.keyboard.candidates.view.k","keyboardType":"swiftkey","keys":[{"x":67,"y":1637,"code":"q"},{"x":211,"y":1637,"code":"w"},{"x":364,"y":1632,"code":"e"},{"x":499,"y":1632,"code":"r"},{"x":648,"y":1637,"code":"t"},{"x":796,"y":1632,"code":"y"},{"x":936,"y":1648,"code":"u"},{"x":1080,"y":1621,"code":"i"},{"x":1219,"y":1642,"code":"o"},{"x":1363,"y":1637,"code":"p"},{"x":139,"y":1866,"code":"a"},{"x":274,"y":1866,"code":"s"},{"x":423,"y":1866,"code":"d"},{"x":571,"y":1840,"code":"f"},{"x":715,"y":1866,"code":"g"},{"x":864,"y":1877,"code":"h"},{"x":999,"y":1877,"code":"j"},{"x":1152,"y":1866,"code":"k"},{"x":1300,"y":1872,"code":"l"},{"x":288,"y":2096,"code":"z"},{"x":441,"y":2096,"code":"x"},{"x":567,"y":2080,"code":"c"},{"x":724,"y":2080,"code":"v"},{"x":864,"y":2085,"code":"b"},{"x":1008,"y":2085,"code":"n"},{"x":1143,"y":2080,"code":"m"},{"x":67,"y":1637,"code":"Q"},{"x":211,"y":1637,"code":"W"},{"x":364,"y":1632,"code":"E"},{"x":499,"y":1632,"code":"R"},{"x":648,"y":1637,"code":"T"},{"x":796,"y":1632,"code":"Y"},{"x":936,"y":1648,"code":"U"},{"x":1080,"y":1621,"code":"I"},{"x":1219,"y":1642,"code":"O"},{"x":1363,"y":1637,"code":"P"},{"x":139,"y":1866,"code":"A"},{"x":274,"y":1866,"code":"S"},{"x":423,"y":1866,"code":"D"},{"x":571,"y":1840,"code":"F"},{"x":715,"y":1866,"code":"G"},{"x":864,"y":1877,"code":"H"},{"x":999,"y":1877,"code":"J"},{"x":1152,"y":1866,"code":"K"},{"x":1300,"y":1872,"code":"L"},{"x":288,"y":2096,"code":"Z"},{"x":441,"y":2096,"code":"X"},{"x":567,"y":2080,"code":"C"},{"x":724,"y":2080,"code":"V"},{"x":864,"y":2085,"code":"B"},{"x":1008,"y":2085,"code":"N"},{"x":1143,"y":2080,"code":"M"},{"x":99,"y":2074,"code":"shift"},{"x":1336,"y":2058,"code":"delete"},{"x":729,"y":2293,"code":"space"},{"x":1345,"y":2320,"code":"enter"}]}
```

可以通过`javaprocess`的Main类生成该json文件，测试时，文件`adb push 文件 /sdcard/layout_info`

#### 键信息 ####
键信息通过`android monkeyrunner`通过`MonkeyRecorder`来收录键盘键的坐标

monkeyrunner路径`sdk目录/sdk/tools/monkeyrunner`

简易monkeyrecoder脚本位于`javaprocess/src/main/script/monkey_recoder.py`

最后可导出坐标文本，如下：

```json
TOUCH|{'x':67,'y':1632,'type':'downAndUp','code':'q'}
TOUCH|{'x':220,'y':1616,'type':'downAndUp','code':'w'}
TOUCH|{'x':360,'y':1616,'type':'downAndUp','code':'e'}
TOUCH|{'x':508,'y':1621,'type':'downAndUp','code':'r'}
TOUCH|{'x':643,'y':1621,'type':'downAndUp','code':'t'}
TOUCH|{'x':787,'y':1621,'type':'downAndUp','code':'y'}
TOUCH|{'x':936,'y':1616,'type':'downAndUp','code':'u'}
TOUCH|{'x':1084,'y':1621,'type':'downAndUp','code':'i'}
TOUCH|{'x':1224,'y':1621,'type':'downAndUp','code':'o'}
TOUCH|{'x':1363,'y':1616,'type':'downAndUp','code':'p'}
#
TOUCH|{'x':67,'y':1632,'type':'downAndUp','code':'Q'}
TOUCH|{'x':220,'y':1616,'type':'downAndUp','code':'W'}
TOUCH|{'x':360,'y':1616,'type':'downAndUp','code':'E'}
TOUCH|{'x':508,'y':1621,'type':'downAndUp','code':'R'}
TOUCH|{'x':643,'y':1621,'type':'downAndUp','code':'T'}
TOUCH|{'x':787,'y':1621,'type':'downAndUp','code':'Y'}
TOUCH|{'x':936,'y':1616,'type':'downAndUp','code':'U'}
TOUCH|{'x':1084,'y':1621,'type':'downAndUp','code':'I'}
TOUCH|{'x':1224,'y':1621,'type':'downAndUp','code':'O'}
TOUCH|{'x':1363,'y':1616,'type':'downAndUp','code':'P'}
#
TOUCH|{'x':135,'y':1845,'type':'downAndUp','code':'a'}
TOUCH|{'x':288,'y':1845,'type':'downAndUp','code':'s'}
TOUCH|{'x':436,'y':1834,'type':'downAndUp','code':'d'}
TOUCH|{'x':580,'y':1824,'type':'downAndUp','code':'f'}
TOUCH|{'x':724,'y':1845,'type':'downAndUp','code':'g'}
TOUCH|{'x':868,'y':1850,'type':'downAndUp','code':'h'}
TOUCH|{'x':1008,'y':1856,'type':'downAndUp','code':'j'}
TOUCH|{'x':1156,'y':1850,'type':'downAndUp','code':'k'}
TOUCH|{'x':1296,'y':1834,'type':'downAndUp','code':'l'}
#
TOUCH|{'x':135,'y':1845,'type':'downAndUp','code':'A'}
TOUCH|{'x':288,'y':1845,'type':'downAndUp','code':'S'}
TOUCH|{'x':436,'y':1834,'type':'downAndUp','code':'D'}
TOUCH|{'x':580,'y':1824,'type':'downAndUp','code':'F'}
TOUCH|{'x':724,'y':1845,'type':'downAndUp','code':'G'}
TOUCH|{'x':868,'y':1850,'type':'downAndUp','code':'H'}
TOUCH|{'x':1008,'y':1856,'type':'downAndUp','code':'J'}
TOUCH|{'x':1156,'y':1850,'type':'downAndUp','code':'K'}
TOUCH|{'x':1296,'y':1834,'type':'downAndUp','code':'L'}
#
TOUCH|{'x':288,'y':2069,'type':'downAndUp','code':'z'}
TOUCH|{'x':436,'y':2058,'type':'downAndUp','code':'x'}
TOUCH|{'x':576,'y':2074,'type':'downAndUp','code':'c'}
TOUCH|{'x':729,'y':2074,'type':'downAndUp','code':'v'}
TOUCH|{'x':868,'y':2058,'type':'downAndUp','code':'b'}
TOUCH|{'x':1012,'y':2069,'type':'downAndUp','code':'n'}
TOUCH|{'x':1152,'y':2069,'type':'downAndUp','code':'m'}
#
TOUCH|{'x':288,'y':2069,'type':'downAndUp','code':'Z'}
TOUCH|{'x':436,'y':2058,'type':'downAndUp','code':'X'}
TOUCH|{'x':576,'y':2074,'type':'downAndUp','code':'C'}
TOUCH|{'x':729,'y':2074,'type':'downAndUp','code':'V'}
TOUCH|{'x':868,'y':2058,'type':'downAndUp','code':'B'}
TOUCH|{'x':1012,'y':2069,'type':'downAndUp','code':'N'}
TOUCH|{'x':1152,'y':2069,'type':'downAndUp','code':'M'}
# 逗号和句号
TOUCH|{'x':283,'y':2293,'type':'downAndUp','code':','}
TOUCH|{'x':1152,'y':2282,'type':'downAndUp','code':'.'}
#shift键
TOUCH|{'x':94,'y':2069,'type':'downAndUp','code':'shift'}
#删除键
TOUCH|{'x':1336,'y':2064,'type':'downAndUp','code':'delete'}
#回车键
TOUCH|{'x':1332,'y':2293,'type':'downAndUp','code':'enter'}
#空格键
TOUCH|{'x':765,'y':2272,'type':'downAndUp','code':'space'}

```

> 注意点：

> 通过MonkeyRecorder获取的坐标的起始点是从屏幕的左上角开始，而MainKeyboardView的坐标起始点是从键盘的左上角开始




### 注意事项

1. hack输入法时代码需要读写sdcard，所以需要需要权限，目前Gboard只提供了读sdcard权限，没有写sdcard权限，代码通过其他方式方式解决，但影响了扩展性，只限Gboard
2. 在指定需要hack的app时，需要运行MainActivity，并输入hack的app包名并保存
> 该包名用于Xposed模块筛选需要hack的app
