import 'package:flutter/material.dart';
import '../models/layer_model.dart';
import '../services/platform_bridge.dart';

class FlutterLayerManagerScreen extends StatefulWidget {
  const FlutterLayerManagerScreen({super.key});

  @override
  State<FlutterLayerManagerScreen> createState() => _FlutterLayerManagerScreenState();
}

class _FlutterLayerManagerScreenState extends State<FlutterLayerManagerScreen> {
  final TextEditingController _searchController = TextEditingController();
  String _searchQuery = '';
  String _selectedFilter = 'all'; // all, point, line, polygon, visible, hidden
  bool _isLoadingNative = false;

  final List<LayerModel> _layers = [
    const LayerModel(
      id: '1',
      name: 'مباني المنطقة المركزية',
      fileType: FileType.shapefile,
      geomType: 'Polygon',
      featureCount: 4200,
      color: Color(0xFFE65100),
      groupName: '📁 مخططات العمران',
      minZoom: 14,
      maxZoom: 22,
    ),
    const LayerModel(
      id: '2',
      name: 'شبكة الطرق الرئيسية',
      fileType: FileType.geoPackage,
      geomType: 'LineString',
      featureCount: 1850,
      color: Color(0xFF0288D1),
      groupName: '📁 البنية التحتية',
      minZoom: 10,
      maxZoom: 22,
    ),
    const LayerModel(
      id: '3',
      name: 'نقاط الآبار والمعالم',
      fileType: FileType.kml,
      geomType: 'Point',
      featureCount: 310,
      color: Color(0xFF43A047),
      groupName: '📁 الموارد المائية',
      minZoom: 1,
      maxZoom: 22,
    ),
  ];

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  List<LayerModel> get _filteredLayers {
    return _layers.where((layer) {
      final matchesSearch = layer.name.toLowerCase().contains(_searchQuery.toLowerCase());
      if (!matchesSearch) return false;

      switch (_selectedFilter) {
        case 'point':
          return layer.geomType?.toLowerCase().contains('point') ?? false;
        case 'line':
          return layer.geomType?.toLowerCase().contains('line') ?? false;
        case 'polygon':
          return layer.geomType?.toLowerCase().contains('poly') ?? false;
        case 'visible':
          return layer.isVisible;
        case 'hidden':
          return !layer.isVisible;
        default:
          return true;
      }
    }).toList();
  }

  void _toggleLayerVisibility(String id) {
    setState(() {
      final index = _layers.indexWhere((l) => l.id == id);
      if (index != -1) {
        _layers[index] = _layers[index].copyWith(isVisible: !_layers[index].isVisible);
      }
    });
  }

  void _toggleGroupVisibility(String groupName, bool isVisible) {
    setState(() {
      for (int i = 0; i < _layers.length; i++) {
        if (_layers[i].groupName == groupName) {
          _layers[i] = _layers[i].copyWith(isVisible: isVisible);
        }
      }
    });
  }

  Future<void> _triggerNativeParse(String filePath, String type) async {
    setState(() => _isLoadingNative = true);
    final result = await PlatformBridge.parseFileNative(filePath: filePath, fileType: type);
    setState(() => _isLoadingNative = false);

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(result['success'] == true ? 'تم استيراد الطبقة بنجاح عبر أندرويد' : 'فشل الاستيراد: ${result['error']}'),
          backgroundColor: result['success'] == true ? Colors.green : Colors.red,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final groupedLayers = <String?, List<LayerModel>>{};
    for (var layer in _filteredLayers) {
      groupedLayers.putIfAbsent(layer.groupName, () => []).add(layer);
    }

    return Scaffold(
      backgroundColor: const Color(0xFF10141C),
      appBar: AppBar(
        backgroundColor: const Color(0xFF161C28),
        elevation: 0,
        title: const Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'مدير الطبقات الذكي (Flutter Hybrid)',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white),
            ),
            Text(
              'الربط الهجين بين فلاتر ومحللات أندرويد السريعة',
              style: TextStyle(fontSize: 12, color: Colors.white60),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.note_add_outlined, color: Color(0xFFFF9800)),
            onPressed: () => _triggerNativeParse('/storage/emulated/0/Download/test.shp', 'Shapefile'),
            tooltip: 'استيراد طبقة جديدة عبر أندرويد',
          ),
        ],
      ),
      body: Directionality(
        textDirection: TextDirection.rtl,
        child: Column(
          children: [
            if (_isLoadingNative)
              const LinearProgressIndicator(color: Color(0xFFFF9800), backgroundColor: Color(0xFF161C28)),
            
            // ── 1. شريط البحث الفوري ──
            Padding(
              padding: const EdgeInsets.all(12.0),
              child: Container(
                decoration: BoxDecoration(
                  color: const Color(0xFF1A2232),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
                ),
                child: TextField(
                  controller: _searchController,
                  onChanged: (val) => setState(() => _searchQuery = val),
                  style: const TextStyle(color: Colors.white),
                  decoration: InputDecoration(
                    hintText: 'البحث عن طبقة بالاسم...',
                    hintStyle: const TextStyle(color: Colors.white38),
                    prefixIcon: const Icon(Icons.search, color: Color(0xFFFF9800)),
                    suffixIcon: _searchQuery.isNotEmpty
                        ? IconButton(
                            icon: const Icon(Icons.clear, color: Colors.white54),
                            onPressed: () {
                              _searchController.clear();
                              setState(() => _searchQuery = '');
                            },
                          )
                        : null,
                    border: InputBorder.none,
                    contentPadding: const EdgeInsets.symmetric(vertical: 14),
                  ),
                ),
              ),
            ),

            // ── 2. شرائح التصفية (Filter Chips) ──
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 12),
              child: Row(
                children: [
                  _buildFilterChip('الكل', 'all'),
                  _buildFilterChip('نقاط 📍', 'point'),
                  _buildFilterChip('خطوط 〰️', 'line'),
                  _buildFilterChip('مضلعات 🔷', 'polygon'),
                  _buildFilterChip('ظاهرة 👁️', 'visible'),
                  _buildFilterChip('مخفية 🙈', 'hidden'),
                ],
              ),
            ),
            const SizedBox(height: 8),

            // ── 3. قائمة الطبقات والمجموعات ──
            Expanded(
              child: _filteredLayers.isEmpty
                  ? const Center(
                      child: Text('لا توجد طبقات تطابق الفلترة الحالية', style: TextStyle(color: Colors.white54)),
                    )
                  : ListView.builder(
                      padding: const EdgeInsets.all(12),
                      itemCount: groupedLayers.keys.length,
                      itemBuilder: (context, index) {
                        final groupName = groupedLayers.keys.elementAt(index);
                        final layers = groupedLayers[groupName]!;

                        if (groupName == null) {
                          return Column(
                            children: layers.map((l) => _buildLayerCard(l)).toList(),
                          );
                        }

                        final allVisible = layers.every((l) => l.isVisible);

                        return Container(
                          margin: const EdgeInsets.only(bottom: 12),
                          decoration: BoxDecoration(
                            color: const Color(0xFF161C28),
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(color: Colors.white.withValues(alpha: 0.06)),
                          ),
                          child: ExpansionTile(
                            leading: Icon(
                              allVisible ? Icons.folder : Icons.folder_outlined,
                              color: const Color(0xFFFF9800),
                            ),
                            title: Text(
                              '$groupName (${layers.length})',
                              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                            ),
                            trailing: IconButton(
                              icon: Icon(
                                allVisible ? Icons.visibility : Icons.visibility_off,
                                color: allVisible ? const Color(0xFFFF9800) : Colors.white38,
                              ),
                              onPressed: () => _toggleGroupVisibility(groupName, !allVisible),
                            ),
                            children: layers.map((l) => _buildLayerCard(l)).toList(),
                          ),
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFilterChip(String label, String key) {
    final isSelected = _selectedFilter == key;
    return Padding(
      padding: const EdgeInsets.only(left: 6),
      child: ChoiceChip(
        label: Text(label),
        selected: isSelected,
        onSelected: (selected) {
          if (selected) setState(() => _selectedFilter = key);
        },
        selectedColor: const Color(0xFFFF9800),
        backgroundColor: const Color(0xFF1E2638),
        labelStyle: TextStyle(
          color: isSelected ? Colors.black : Colors.white70,
          fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
        ),
      ),
    );
  }

  Widget _buildLayerCard(LayerModel layer) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFF1C2434),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: layer.color.withValues(alpha: 0.3)),
      ),
      child: Column(
        children: [
          Row(
            children: [
              Container(
                width: 12,
                height: 12,
                decoration: BoxDecoration(color: layer.color, shape: BoxShape.circle),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  layer.name,
                  style: TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                    decoration: layer.isVisible ? TextDecoration.none : TextDecoration.lineThrough,
                  ),
                ),
              ),
              IconButton(
                icon: Icon(
                  layer.isVisible ? Icons.visibility : Icons.visibility_off,
                  color: layer.isVisible ? const Color(0xFFFF9800) : Colors.white38,
                ),
                onPressed: () => _toggleLayerVisibility(layer.id),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Row(
            children: [
              _buildBadge(layer.fileType.label, Colors.blueAccent),
              const SizedBox(width: 6),
              _buildBadge('🔍 z${layer.minZoom}-z${layer.maxZoom}', Colors.purpleAccent),
              const SizedBox(width: 6),
              _buildBadge('${layer.featureCount} معلم', Colors.teal),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildBadge(String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        text,
        style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold),
      ),
    );
  }
}
