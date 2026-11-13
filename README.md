# Camry 桌面（Android 車機 Launcher）

仿照 Toyota Camry（第 9 代）12.3 吋多媒體系統的介面結構，做給 Android 車機用的桌面。
目標機：智乘進階機（UIS7870SC、Android 13、2000×1200）。與 Toyota 無關，沒有使用任何 Toyota 標誌。

依據手冊與實車照片的部分：只有圖示的左側主選單（選中為藍色圓底）、左下角浮動麥克風、
右上角狀態圖示與 12 小時制時間、清單畫面的「子選單＋主要區域」結構、外框膠囊按鈕、
音訊的「來源」全螢幕選擇畫面。顏色是從照片取的近似值，不是原廠色碼。

## 功能
| 項目 | 說明 |
|---|---|
| 主選單 | CarPlay*、Android Auto*、主頁、導航、音訊、電話、車輛資訊、應用程式、設定（*見下方「CarPlay 偵測」） |
| 主頁 | 導航大卡片、迷你播放器、4 個常用 App（長按更換） |
| 音訊 | 播放畫面：歌名、歌手、專輯、封面、進度、上下首、播放／暫停；麥克風旁的「來源」開啟全螢幕來源選擇 |
| 狀態列 | 時間（12／24 小時制可選）、藍牙、行動網路、Wi-Fi |
| 日夜配色 | 自動（可設日間／夜間起始小時）、日間、夜間；只改桌面配色，不動螢幕亮度 |
| App 綁定 | 每個入口可指定要開哪個 App；第一次開啟會依名稱猜測 |

不需 root、不刷機、不使用 `QUERY_ALL_PACKAGES`。唯一的特殊權限是「通知存取」，用途有兩個：
讀 MediaSession（目前播放的音樂），以及看哪些 App 掛著常駐通知（偵測 CarPlay 連線）。
通知標題只讀綁定為 CarPlay／Android Auto 的 App，只放在記憶體、只顯示在偵測狀態裡。

### CarPlay 偵測
跟實車一樣，CarPlay／Android Auto 只在**有綁定且偵測到連線**時出現在左側選單最上面。
本桌面無法直接得知 CarLink 的連線狀態，用兩個訊號判斷，任一成立就算連線：
- USB：有線 CarPlay 時 USB 上有 Apple 裝置（vendor `05AC`）；有線 Android Auto 時有 Android 配件模式裝置（`18D1:2D00`–`2D05`）。
- 常駐通知：綁定的 App（例如 CarLink）掛著常駐／前景服務通知。

偵測不準時，可到 設定 → 顯示 改成「有綁定就一律顯示」。實際偵測結果在 設定 → 權限與系統 → CarPlay／Android Auto 偵測狀態。

## 安裝與測試
1. 把 `app-debug.apk` 放到隨身碟，在車機用檔案管理開啟安裝（需允許未知來源）。
2. **先不要設為預設桌面**，從原廠桌面點「Camry 桌面」圖示開啟。
3. 設定 → 權限與系統 → 開啟通知存取。如果跳出「受限制的設定」（Android 13 對自行安裝 App 的限制），先按「開啟本 App 資訊」，點右上角 ⋮ →「允許受限制的設定」，再回來開通知存取。
   若車機的 App 資訊頁沒有 ⋮ 選單，可用 adb：`adb shell cmd notification allow_listener tw.camry.launcher/tw.camry.launcher.media.MediaListenerService`。
4. 設定 → App 綁定：確認導航、電話、音訊來源、CarPlay（CarLink）、Android Auto 都綁對。
5. 連上／拔掉 iPhone，看 設定 → 權限與系統 → CarPlay 偵測狀態 是否跟著變化；不準的話拍照回報。
6. 確認倒車影像、方向盤按鍵、Home 鍵都正常後，再到「變更預設桌面」選 Camry 桌面。

退路：桌面內「設定 → 權限與系統 → 變更預設桌面」，或系統設定 → 應用程式 → 預設應用程式 → 主畫面應用程式，或直接解除安裝。

## 編譯
- Android Studio 開啟此資料夾後 Build APK，或 `./gradlew assembleDebug`，輸出在 `app/build/outputs/apk/debug/`。
- 推到 GitHub 後 Actions（`.github/workflows/build-apk.yml`）會跑單元測試並產出 APK artifact。

## 已知限制
- 尚未在實機測試，介面也只通過編譯，沒有在實機或模擬器上目視檢查。
- CarPlay 偵測未在實車驗證：CarLink 可能一直掛著常駐通知（會變成一直顯示），或無線 CarPlay 時沒有任何訊號（會變成不顯示）。
- 沒有實車 CarPlay 的「放大／縮小」（隱藏左側選單）：CarLink 開啟時是全螢幕，桌面收不到 CarPlay 畫面裡的按鈕。
- 沒有分割畫面導航、沒有地圖嵌入（第三方導航 App 無法嵌入）。
- 車輛資訊要自行綁定一個 App；沒綁定會提示。
- release 暫用 debug 簽章，僅供個人側載；之後覆蓋安裝請維持同一把簽章。
