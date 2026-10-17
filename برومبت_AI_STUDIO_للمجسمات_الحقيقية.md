# برومبت AI Studio لتوليد مجسمات اللاعبين 3D الحقيقية ⚽

هذا الملف يحتوي على برومبت متخصص يمكنك نسخه ولصقه في أدوات التوليد ثلاثية الأبعاد (مثل AI Studio 3D / Meshy / Tripo3D) لإنشاء موديلات رياضية بصيغة `GLB` مع ملامح واقعية:

---

### نص البرومبت (Prompt):
```text
Photorealistic 3D full-body athletic soccer player in match action stance, high-poly stylized character, dynamic muscular anatomy, modern aerodynamic cyber-tactical neon jersey with customizable number on chest and back, realistic cleats, dynamic lighting with neon blue and cyan rim reflections, optimized topology rigged for 360-degree rotation, export as optimized GLB/gTF format for mobile realtime rendering.
```

---

### كيفية ربط مجسمات الـ GLB بالتطبيق:
1. يتم حفظ ملفات الـ `player.glb` داخل مجلد `app/src/main/assets/models/`.
2. في شاشة `Player3DViewer.kt`، يتم استدعاء المجسم عبر محرك الرسم أو عبر مكون Google Filament / Sceneview 3D المتوافق مع Jetpack Compose.
3. التدوير والإمالة والتكبير تعمل بزاوية 360 درجة مع مؤشر الحالة البدنية والنفسية (PCI).
