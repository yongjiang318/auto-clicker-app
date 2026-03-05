import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const AutoClickerApp());
}

class AutoClickerApp extends StatelessWidget {
  const AutoClickerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '自动点击器',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  static const platform = MethodChannel('com.example.auto_clicker/accessibility');
  bool _isAccessibilityEnabled = false;
  List<Map<String, dynamic>> _clickActions = [];
  bool _isAutoClicking = false;

  @override
  void initState() {
    super.initState();
    _checkAccessibilityStatus();
  }

  Future<void> _checkAccessibilityStatus() async {
    try {
      final bool isEnabled = await platform.invokeMethod('isAccessibilityEnabled');
      setState(() {
        _isAccessibilityEnabled = isEnabled;
      });
    } catch (e) {
      debugPrint('检查无障碍服务失败: $e');
    }
  }

  Future<void> _openAccessibilitySettings() async {
    try {
      await platform.invokeMethod('openAccessibilitySettings');
    } catch (e) {
      debugPrint('打开设置失败: $e');
    }
  }

  Future<void> _addClickAction() async {
    final result = await showDialog<Map<String, dynamic>>(
      context: context,
      builder: (context) => AddClickActionDialog(),
    );

    if (result != null) {
      setState(() {
        _clickActions.add(result);
      });
    }
  }

  Future<void> _startAutoClick() async {
    if (_clickActions.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('请先添加点击动作')),
      );
      return;
    }

    try {
      final bool success = await platform.invokeMethod('startAutoClick', {
        'actions': _clickActions,
      });

      if (success) {
        setState(() {
          _isAutoClicking = true;
        });

        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('自动点击已启动')),
        );

        // 模拟自动执行
        await _executeClickActions();

        setState(() {
          _isAutoClicking = false;
        });
      }
    } catch (e) {
      debugPrint('启动自动点击失败: $e');
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('启动失败: $e')),
      );
    }
  }

  Future<void> _stopAutoClick() async {
    try {
      await platform.invokeMethod('stopAutoClick');
      setState(() {
        _isAutoClicking = false;
      });
    } catch (e) {
      debugPrint('停止自动点击失败: $e');
    }
  }

  Future<void> _executeClickActions() async {
    for (var action in _clickActions) {
      if (!_isAutoClicking) break;

      try {
        await platform.invokeMethod('performClick', action);
        await Future.delayed(const Duration(milliseconds: 500));
      } catch (e) {
        debugPrint('执行点击失败: $e');
      }
    }
  }

  void _removeAction(int index) {
    setState(() {
      _clickActions.removeAt(index);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🤖 自动点击器'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: _openAccessibilitySettings,
            tooltip: '打开无障碍设置',
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 无障碍服务状态
            Card(
              color: _isAccessibilityEnabled ? Colors.green[50] : Colors.orange[50],
              child: ListTile(
                leading: Icon(
                  _isAccessibilityEnabled ? Icons.check_circle : Icons.warning,
                  color: _isAccessibilityEnabled ? Colors.green : Colors.orange,
                ),
                title: Text(
                  _isAccessibilityEnabled ? '无障碍服务已启用' : '无障碍服务未启用',
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
                subtitle: Text(
                  _isAccessibilityEnabled
                      ? '可以开始自动点击'
                      : '点击右上角图标启用服务',
                ),
              ),
            ),

            const SizedBox(height: 20),

            // 状态信息
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '📊 状态信息',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 10),
                    Text('已添加动作: ${_clickActions.length}'),
                    Text('自动点击状态: ${_isAutoClicking ? "运行中" : "停止"}'),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 20),

            // 点击动作列表
            Text(
              '📋 点击动作列表',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 10),
            Expanded(
              child: _clickActions.isEmpty
                  ? Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            Icons.touch_app,
                            size: 64,
                            color: Colors.grey[400],
                          ),
                          const SizedBox(height: 10),
                          Text(
                            '暂无点击动作',
                            style: TextStyle(color: Colors.grey[600]),
                          ),
                          Text(
                            '点击下方按钮添加',
                            style: TextStyle(
                              color: Colors.grey[500],
                              fontSize: 12,
                            ),
                          ),
                        ],
                      ),
                    )
                  : ListView.builder(
                      itemCount: _clickActions.length,
                      itemBuilder: (context, index) {
                        final action = _clickActions[index];
                        return Card(
                          margin: const EdgeInsets.only(bottom: 8),
                          child: ListTile(
                            leading: const CircleAvatar(
                              child: Icon(Icons.touch_app),
                            ),
                            title: Text('动作 #${index + 1}'),
                            subtitle: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                if (action['type'] == 'coordinate')
                                  Text(
                                    '坐标: (${action['x']}, ${action['y']})',
                                  )
                                else if (action['type'] == 'text')
                                  Text('文本: ${action['text']}'),
                                Text(
                                  '延迟: ${action['delay'] ?? 0}ms',
                                  style: const TextStyle(fontSize: 12),
                                ),
                              ],
                            ),
                            trailing: IconButton(
                              icon: const Icon(Icons.delete),
                              onPressed: () => _removeAction(index),
                            ),
                          ),
                        );
                      },
                    ),
            ),

            // 操作按钮
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: _addClickAction,
                    icon: const Icon(Icons.add),
                    label: const Text('添加点击动作'),
                    style: ElevatedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 16),
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: _isAutoClicking ? _stopAutoClick : _startAutoClick,
                    icon: Icon(_isAutoClicking ? Icons.stop : Icons.play_arrow),
                    label: Text(_isAutoClicking ? '停止' : '开始'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: _isAutoClicking ? Colors.red : Colors.green,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 16),
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class AddClickActionDialog extends StatefulWidget {
  const AddClickActionDialog({super.key});

  @override
  State<AddClickActionDialog> createState() => _AddClickActionDialogState();
}

class _AddClickActionDialogState extends State<AddClickActionDialog> {
  String _type = 'coordinate';
  final TextEditingController _xController = TextEditingController();
  final TextEditingController _yController = TextEditingController();
  final TextEditingController _textController = TextEditingController();
  final TextEditingController _delayController = TextEditingController(text: '500');

  @override
  void dispose() {
    _xController.dispose();
    _yController.dispose();
    _textController.dispose();
    _delayController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('添加点击动作'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 点击类型选择
            const Text('点击类型:'),
            const SizedBox(height: 8),
            SegmentedButton<String>(
              segments: const [
                ButtonSegment(
                  value: 'coordinate',
                  label: Text('坐标点击'),
                  icon: Icon(Icons.crop_free),
                ),
                ButtonSegment(
                  value: 'text',
                  label: Text('文本点击'),
                  icon: Icon(Icons.text_fields),
                ),
              ],
              selected: {_type},
              onSelectionChanged: (Set<String> newSelection) {
                setState(() {
                  _type = newSelection.first;
                });
              },
            ),

            const SizedBox(height: 16),

            // 坐标输入
            if (_type == 'coordinate') ...[
              TextField(
                controller: _xController,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'X 坐标',
                  hintText: '例如: 500',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: _yController,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'Y 坐标',
                  hintText: '例如: 1000',
                  border: OutlineInputBorder(),
                ),
              ),
            ] else if (_type == 'text') ...[
              TextField(
                controller: _textController,
                decoration: const InputDecoration(
                  labelText: '文本内容',
                  hintText: '例如: 登录',
                  border: OutlineInputBorder(),
                ),
              ),
            ],

            const SizedBox(height: 16),

            // 延迟时间
            TextField(
              controller: _delayController,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: '延迟时间（毫秒）',
                hintText: '例如: 500',
                border: OutlineInputBorder(),
              ),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('取消'),
        ),
        ElevatedButton(
          onPressed: () {
            final action = {
              'type': _type,
              'delay': int.tryParse(_delayController.text) ?? 500,
            };

            if (_type == 'coordinate') {
              action['x'] = int.tryParse(_xController.text) ?? 0;
              action['y'] = int.tryParse(_yController.text) ?? 0;
            } else if (_type == 'text') {
              action['text'] = _textController.text.trim();
            }

            Navigator.pop(context, action);
          },
          child: const Text('添加'),
        ),
      ],
    );
  }
}
