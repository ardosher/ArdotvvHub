# ArdoTv Hub

Artel (Android TV) televizorlari uchun ilovalar markazi. To'liq pult (D-pad) bilan boshqariladi — touchscreen shart emas.

## Nima o'zgardi (v1.2)

- **Ekran saqlovchi** — pult bir necha daqiqa tegilmasa, qora fonda sekin suzuvchi ranglar, katta soat, sana va ob-havo chiqadi (soat har 20 soniyada sekin siljiydi). Istalgan tugma uni yopadi. Sozlamalar > "Ekran saqlovchi" (O'chiq / 1 / 2 / 5 / 10 / 15 daqiqa). Ochiq dialog ustidan chiqmaydi.
- **Reklama bannerlari** — bosh sahifa tepasida katta banner qatori. `apps.json` ichidagi `"banners"` massivi orqali boshqariladi (quyida). Bosilganda ilova ochiladi, `action` bajariladi yoki havola ochiladi.
- **"Yangi" belgisi** — `apps.json`da ilovaga `"added": "2026-10-05"` yozsangiz, shu sanadan boshlab 7 kun davomida kartochkada sariq "Yangi" yorlig'i avtomatik chiqadi, keyin o'zi yo'qoladi.
- **Kategoriya belgilari** — har bir bo'lim nomi oldida kichik belgi (🎬 Kino, 🎵 Musiqa, 📺 O'zbek TV, 🛠 Vositalar va h.k.). Nomga qarab avtomatik tanlanadi; xohlasangiz kategoriyaga `"icon": "⚽"` yozib o'zgartirasiz.
- **Ovoz indikatori** — pultdagi Volume tugmalari bosilganda ekran pastida ovoz darajasi chiqadi (Sozlamalarda o'chirsa bo'ladi). Ovozni tizimning o'zi o'zgartiradi, indikator faqat natijani ko'rsatadi. Ovoz "qat'iy" (fixed) qurilmalarda indikator chiqmaydi.
- **Profillar** — sarlavhadagi profil tugmasi yoki Sozlamalar > "Profillar". Har bir profilning o'z sevimlilari, oxirgi ochilganlari va sozlamalari bor (Ota-ona, Ona, Ota, Bolalar, Mehmon yoki "Profil N"). Eski sevimlilar va sozlamalar birinchi profilda (Ota-ona) saqlanib qoladi. Eng ko'pi 6 ta profil.
- **Ovozli qidiruv** — sarlavhadagi 🎤 tugmasi yoki pultdagi mikrofon/qidiruv tugmasi. Android'ning o'zi ovozni matnga aylantiradi va shu matn bilan qidiruv ochiladi. Til: Sozlamalar > "Ovozli qidiruv tili" (O'zbekcha / Ruscha / Inglizcha / Tizim tili). Muhim: ba'zi pultlarning mikrofon tugmasi to'g'ridan-to'g'ri Google Assistant'ni ochadi va ilovaga umuman yetib kelmaydi — bunday holda sarlavhadagi 🎤 tugmasidan foydalaning. Televizorda ovozni aniqlash xizmati bo'lmasa, oddiy qidiruv ochiladi.
- **Ilova va qurilma haqida** — Sozlamalar > "Ilova haqida": versiya, paket nomi, qurilma modeli, Android versiyasi, bo'sh/umumiy xotira (saqlash va RAM) chiziqcha bilan.
- **Xatoliklar jurnali** — "Ilova haqida" ichida. `apps.json` o'qilmasa, ob-havo/banner rasmi yuklanmasa, ilova ochilmasa yoki ilova kutilmaganda yopilsa (crash), sababi shu yerga yoziladi. Takrorlanuvchi bir xil xatolar qayta yozilmaydi. Yozuvni bosib to'liq matnini ko'rasiz; "Jurnalni tozalash" bor.
- **Ob-havo** — sarlavhada harorat va shahar (Open-Meteo, API kalit kerak emas, internet kerak). 30 daqiqada bir yangilanadi; internet vaqtincha yo'q bo'lsa oxirgi qiymat (6 soatgacha) ko'rinadi. Shahar: Sozlamalar > "Ob-havo shahri" (Toshkent, Samarqand, Buxoro, Andijon, Namangan, Farg'ona, Nukus, Qarshi, Termiz, Urganch, Jizzax, Guliston, Navoiy). Ob-havo ekran saqlovchida ham ko'rinadi.
- Ilova endi `INTERNET` ruxsatini so'raydi (ob-havo va banner rasmlari uchun).

### Banner qo'shish (`apps.json`)

Fayl boshidagi `"banners"` massiviga yozing:

```json
"banners": [
  {
    "title": "Yangi film: ...",
    "subtitle": "Bugundan Movieark'da",
    "color": "#B20710",
    "image": "https://.../poster.jpg",
    "packages": ["com.example.movieark"],
    "url": "https://movieark.example/film",
    "until": "2026-12-31"
  }
]
```

| Maydon | Tavsif |
|---|---|
| `title` | Sarlavha (majburiy) |
| `subtitle` | Ixtiyoriy — pastroqdagi kichik matn |
| `color` | Rasm bo'lmasa yoki yuklanmaguncha ko'rinadigan fon rangi |
| `image` | Ixtiyoriy — rasm havolasi. **https** ishlating (oddiy http Android'da bloklanadi). Rasm 6 MB dan kichik bo'lsin, eni 1280 px atrofida yetarli |
| `packages` | Ixtiyoriy — bosilganda ochiladigan ilova. O'rnatilmagan bo'lsa `url`, u ham bo'lmasa Play Market |
| `action` | Ixtiyoriy — `hub_settings`, `search`, `voice`, `all_apps`, `wifi`... (ilova ichidagi amal) |
| `url` | Ixtiyoriy — havola |
| `until` | Ixtiyoriy — shu kunning oxirigacha ko'rsatiladi (`yyyy-MM-dd`), keyin banner o'zi yo'qoladi |

Eslatma: `apps.json` ilova ichida (assets) turadi, shuning uchun banner yoki "Yangi" belgisini o'zgartirish uchun fayl o'zgartirilib, APK qayta yig'iladi.

## Nima o'zgardi (v1.1)

Bosh sahifa endi quyidagi bo'limlardan iborat (bo'sh bo'lim avtomatik yashiriladi):

- **Tavsiya etilganlar** — `apps.json`da `featured: true` bo'lgan ilovalar
- **Sevimli ilovalar** — foydalanuvchi qo'shgan ilovalar (SharedPreferences'da saqlanadi)
- **Tomosha qilishni davom ettiring** — kelajakda kino/serial integratsiyasi uchun tayyor arxitektura (`ContinueWatchingRepository`), hozircha bo'sh
- **Oxirgi ochilganlar** — oxirgi 10 ta, takrorlanmaydi, o'chirilgan ilova avtomatik chiqib ketadi
- `apps.json` kategoriyalari (Asosiy, Kino va seriallar, O'zbek TV va h.k.)
- **Kirishlar (HDMI)** — televizor qo'llab-quvvatlasa
- **Tezkor tugmalar** — `apps.json`dagi `quick` ro'yxati (Qidiruv, Barcha ilovalar, Wi-Fi, Sozlamalar)

Sarlavhada: logotip, soat, sana, Wi-Fi holati (signal darajasi bilan), internet holati — barchasi Sozlamalardan yoqib/o'chirib bo'ladi.

### Yangi imkoniyatlar

- **Qidiruv** — ekran klaviaturasi va pult/fizik klaviatura bilan real-time qidiruv
- **Barcha ilovalar** — alifbo tartibida, grid ko'rinishida, alohida oynada
- **Ilova kontekst menyusi** — uzoq OK yoki pultdagi MENU tugmasi: Ochish, Sevimlilarga qo'shish/olib tashlash, Ilova haqida, Play Market, O'chirish (rasmiy uninstall oynasi orqali; tizim ilovalari himoyalangan)
- **Ilova haqida** oynasi — icon, paket nomi, holati, versiya nomi/kodi, o'rnatilgan sana
- **ArdoTv Hub sozlamalari** — sevimlilarni boshqarish, kartochka o'lchami, Dark/Light/Automatic tema, 5 xil fon gradienti, animatsiya/soat/sana/Wi-Fi/internet ko'rsatish sozlamalari, avtomatik refresh, launcher haqida ma'lumot, versiya
- Internet holati `ConnectivityManager.NetworkCallback` orqali real vaqtda kuzatiladi
- Ilova ikonkalari xotirada keshlanadi (memory leak yo'q — faqat `applicationContext` ushlanadi)

Mavjud funksiyalar (launcher rejimi, HOME intent-filter, `TvInputManager` orqali HDMI, `apps.json` formati) o'zgarishsiz saqlangan — faqat kengaytirilgan.

## APK ni GitHub'da yig'ish

1. github.com da yangi repozitoriy oching (masalan `ArdoTvHub`).
2. Shu papkadagi hamma fayllarni (`.github` papkasi bilan) repozitoriyga yuklang.
3. Repozitoriyda **Actions** bo'limiga kiring. "APK yig'ish" ishga tushadi (2-5 daqiqa).
4. Tugagach, ish sahifasi pastidagi **Artifacts** dan `ArdoTvHub-apk` ni yuklab oling (zip ichida `app-debug.apk`).
5. APK ni Telegram orqali yuboring, televizorda yuklab olib o'rnating (Sozlamalar > Xavfsizlik > Noma'lum manbalar).

## Ilova qo'shish/olib tashlash

`app/src/main/assets/apps.json` faylini tahrirlang. Har bir ilova quyidagi maydonlarni qo'llab-quvvatlaydi:

| Maydon | Tavsif |
|---|---|
| `name` | Kartochkadagi nom |
| `packages` | Ilovaning paket nomi(lari) — bir nechta variant yozish mumkin |
| `color` | Kartochka rangi (`#RRGGBB`) |
| `url` | Ixtiyoriy — o'rnatilmagan bo'lsa ochiladigan havola |
| `action` | Ixtiyoriy — `wifi`, `settings`, `search`, `all_apps`, `hub_settings`, `bluetooth`, `display`, `sound`, `network`, `system`, yoki `hdmi:<id>` (avtomatik), `voice` (ovozli qidiruv), `profile` (profillar oynasi) |
| `description` | Ixtiyoriy — "Ilova haqida" oynasida ko'rinadi |
| `featured` | `true` bo'lsa "Tavsiya etilganlar"da chiqadi |
| `added` | Ixtiyoriy — `yyyy-MM-dd`. Shu sanadan 7 kun davomida kartochkada "Yangi" belgisi chiqadi |
| `hidden` | `true` bo'lsa bosh sahifada yashiriladi |

Paket nomi bo'sh qoldirilsa, o'rnatilmagan holatda Play Market'da nom bo'yicha qidiriladi.

Kategoriyaga ixtiyoriy `"icon"` (emoji) yozish mumkin. Yuqori darajadagi ixtiyoriy `"quick"` massivi bosh sahifadagi tezkor tugmalar qatorini boshqaradi (masalan Qidiruv, Barcha ilovalar, Wi-Fi, Sozlamalar).

## Qo'shimcha imkoniyatlar

- **Oxirgi ochilganlar**: eng oxirgi ochilgan ilova birinchi o'rinda, maks. 10 ta.
- **Kirishlar (HDMI)**: televizor HDMI kirishlari kartochka bo'lib chiqadi (televizor qo'llab-quvvatlasa).
- **Barcha ilovalar**: televizorda o'rnatilgan hamma ilova avtomatik ko'rinadi.
- **Launcher rejimi**: Home tugmasi bosilganda "ArdoTv Hub" ni tanlab, "Doim" ni bossangiz, televizor yoqilganda shu ilova ochiladi. Eski holatga qaytarish: Sozlamalar > Ilovalar > ArdoTv Hub > "Standart sozlamalarni tozalash".
- **Sozlamalar**: ArdoTv Hub ichidagi "ArdoTv Hub sozlamalari" kartasidan barcha ko'rinish va xatti-harakat sozlamalarini boshqarish mumkin.

## Loyiha arxitekturasi

- `MainActivity` — bosh ekran, bo'limlarni yig'ish va yangilash, `HubActions` orqali ichki oynalarni ochish
- `HeaderView` — sarlavha (logotip, soat, sana, Wi-Fi/internet, tezkor tugmalar)
- `AppAdapter` — kartochkalar, DiffUtil bilan optimallashtirilgan yangilanish, uzoq OK/MENU orqali kontekst menyu
- `AppEntry` / `Category` / `CardModel` — ma'lumot modellari
- `AppRepository` — `apps.json`ni xavfsiz o'qish (`loadSafe`, xato bo'lsa crash qilmaydi)
- `AppLauncher` — ilovani ochish, action'lar, Play Market, o'chirish
- `InstalledApps` — o'rnatilgan ilovalarni skanerlash (fon oqimida chaqiriladi)
- `IconCache` — ikonkalarni xotirada keshlash
- `NetworkMonitor` — Wi-Fi/internet holatini kuzatish
- `Prefs` — barcha SharedPreferences (sevimlilar, oxirgilar, sozlamalar)
- `HubTheme` — Dark/Light/Automatic tema va fon gradientlari
- `Dialogs` — barcha ichki oynalar: kontekst menyu, ilova haqida, qidiruv, barcha ilovalar grid, sozlamalar, sevimlilarni boshqarish
- `ContinueWatching` — "Tomosha qilishni davom ettiring" uchun tayyor (hozircha bo'sh) arxitektura
- `ProfileManager` / `Profile` — profillar ro'yxati, faol profil (har biriga alohida `Prefs` fayli)
- `ScreensaverView` — ekran saqlovchi; `VolumeIndicator` — ovoz indikatori
- `BannerAdapter` (+ `BannerImages`) — reklama bannerlari va ularning rasmlari
- `Weather` — ob-havo (Open-Meteo), keshlash; `DeviceInfo` — xotira va versiya ma'lumoti
- `ErrorLog` — xatoliklar jurnali; `HubApp` — crash'larni jurnalga yozadi
- `CategoryIcons` — kategoriya belgilari
