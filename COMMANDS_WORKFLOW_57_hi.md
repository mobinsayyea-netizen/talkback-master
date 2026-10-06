# बाकी कमांड (57 में से 27 बन चुके, 30 बाकी) और उनका वर्कफ़्लो

स्रोत: REMAINING_COMMANDS.txt (116 / 173 बने)। हर कमांड ठीक एक जगह है। नीचे हर कमांड का अर्थ उसके नाम से लिया गया है। जिनमें अर्थ साफ़ नहीं है उन्हें समूह H में रखा है।

## वर्कफ़्लो (क्रम)

1. समूह A, फिर B: आसान, तेज़ बनते हैं।
2. समूह C: मौजूदा स्क्रीनशॉट/OCR कोड पर।
3. समूह D और E: नेविगेशन और टेक्स्ट, आपस में जुड़े।
4. समूह G: बोलकर इनपुट।
5. समूह F: सबसे कठिन, आख़िर में।
6. समूह H: आपके फ़ैसले मिलते ही।

हर चरण के बाद: zip, GitHub Actions पर बिल्ड, एरर लॉग, सुधार, फिर अगला चरण।

## संख्या

| समूह | कमांड |
|---|---|
| A. स्क्रीन या सूची खोलने वाले (आसान) | 7 |
| B. चालू/बंद स्विच | 9 |
| C. पहचान और स्क्रीनशॉट | 9 |
| D. नेविगेशन और ब्राउज़िंग के मोड | 12 |
| E. टेक्स्ट एडिटिंग और कॉपी | 4 |
| F. फ़ोकस की निगरानी और वर्चुअल स्क्रीन (कठिन) | 5 |
| G. बोलकर इनपुट और चैट पढ़ना | 5 |
| H. पहले आपका फ़ैसला चाहिए | 6 |
| कुल | 57 |

## A. स्क्रीन या सूची खोलने वाले (आसान)

सिर्फ़ एक स्क्रीन, सूची या सिस्टम पिकर खोलना। पहले कर लें, क्योंकि बाकी स्टेप इन पर टिके हैं।

1. (Step 10 में बना) `directory_tools`: Tools फ़ोल्डर की सूची खोलना और चुना हुआ टूल चलाना (एक्सटेंशन की सूची जैसा)
2. (Step 10 में बना) `tool_title`: Tools: वही सूची, दूसरे नाम से
3. (Step 10 में बना) `directory_timer`: Timer: टाइमर सेट करने की स्क्रीन
4. `profiles_title`: Profiles: प्रोफ़ाइल की सूची (क्या मतलब है, यह आप तय करें)
5. `chat_history`: Chat history: पढ़े हुए चैट संदेशों की सूची दिखाना
6. (Step 10 में बना) `input_method`: Input: सिस्टम का कीबोर्ड बदलने वाला पिकर खोलना
7. `command_notification_box`: Notification box: आई हुई सूचनाओं की सूची की स्क्रीन

## B. चालू/बंद स्विच

एक सेटिंग या फ़्लैग बदलना और नई स्थिति बोलना। हर एक के लिए अपनी जगह जुड़ेगी।

1. `command_start_log`: Enable logging: लॉग लिखना चालू/बंद
2. (Step 13 में बना) `command_false_touch_prevention`: Lock touch: गलत स्पर्श रोकने के लिए टच लॉक
3. `command_disable_edge_gesture_title`: Suspend edge gestures: किनारे वाले जेस्चर रोकना
4. (Step 13 में बना) `command_disable_main_tts`: Suspend main TTS engine: मुख्य TTS बंद करके दूसरे से बोलना
5. `command_incall_mode`: Earpiece mode: आवाज़ कान वाले स्पीकर से
6. `switch_multi_finger_gestures`: Toggle multi-finger gestures: बहु-उँगली जेस्चर चालू/बंद
7. `command_pause_a11y_volume`: Suspend accessibility volume: एक्सेसिबिलिटी वॉल्यूम रोकना
8. (Step 13 में बना) `command_pause_gesture`: Suspend screen reader gestures: जेस्चर रोकना
9. (Step 13 में बना) `command_disable_feedback`: Suspend speech feedback and browse by touch: बोलना और टच ब्राउज़ दोनों रोकना

## C. पहचान और स्क्रीनशॉट

स्क्रीनशॉट लेकर उससे पढ़ना या पूछना। Step 8 का स्क्रीनशॉट कोड और मौजूदा OCR/Gemini काम आएँगे।

1. (Step 11 में बना) `command_take_screenshot2`: Take a screenshot of current focus: सिर्फ़ फ़ोकस वाले हिस्से का स्क्रीनशॉट
2. (Step 11 में बना) `command_ocr_text`: Recognize text on the current screen: स्क्रीन का लिखा पढ़ना
3. `use_ocr_title`: Automatically use OCR: जहाँ टेक्स्ट न मिले वहाँ अपने आप OCR
4. (Step 11 में बना) `command_recognition_caption`: Recognize subtitles: सबटाइटल पहचानना
5. (Step 11 में बना) `command_verification`: Get CAPTCHA: कैप्चा पढ़ना
6. (Step 11 में बना) `command_virtual_table`: Identify form: फ़ॉर्म की पहचान
7. `command_auto_trans`: Automatic translation: पढ़े जाने वाले टेक्स्ट का अपने आप अनुवाद
8. (Step 11 में बना) `voice_picture_description`: Inquire by voice about current focus: फ़ोकस के बारे में बोलकर पूछना
9. (Step 11 में बना) `voice_picture_description2`: Inquire by voice about the entire screen: पूरी स्क्रीन के बारे में बोलकर पूछना

## D. नेविगेशन और ब्राउज़िंग के मोड

फ़ोकस चलाने के नए तरीके। आपस में जुड़े हैं, इसलिए एक साथ बनें।

1. (Step 12 में बना) `select_granularity_mode`: Select navigation type: ग्रेन्युलैरिटी चुनना
2. (Step 12 में बना) `to_next_granularity_item`: Next element of the specified type: चुने प्रकार का अगला
3. (Step 12 में बना) `to_previous_granularity_item`: Previous element of the specified type: चुने प्रकार का पिछला
4. `split_menu_title`: Granular browsing mode: बारीक ब्राउज़िंग
5. `split_edit_title`: Granular editing mode: बारीक एडिटिंग
6. `list_mode_title`: List browsing: सूची ब्राउज़िंग
7. `command_quick_mode`: Quick browsing: तेज़ ब्राउज़िंग
8. `command_supper_mode`: Enhanced detailed focusing mode: विस्तृत फ़ोकस मोड
9. `command_tap_mode`: Tapping to move focus mode: टैप से फ़ोकस ले जाना
10. `command_up_tap`: Lift to activate: उँगली उठाने पर क्लिक
11. `command_grid_mode`: Screen Grid mode: स्क्रीन को ग्रिड में बाँटना
12. (Step 12 में बना) `auto_previous`: Backwards automatic browsing: पीछे की ओर अपने आप ब्राउज़

## E. टेक्स्ट एडिटिंग और कॉपी

टेक्स्ट पर काम करने वाले कमांड।

1. (Step 12 में बना) `command_long_edit`: edit current text: मौजूदा टेक्स्ट एडिट करना
2. (Step 12 में बना) `all_text_title`: Full text editing: पूरे टेक्स्ट की एडिटिंग
3. (Step 12 में बना) `command_long_copy`: List copying: सूची की चीज़ें कॉपी करना
4. (Step 12 में बना) `command_long_hold`: Long press and hold: देर तक दबाए रखना

## F. फ़ोकस की निगरानी और वर्चुअल स्क्रीन (कठिन)

स्क्रीन का ढाँचा पढ़ने और बदलाव पर नज़र रखने वाले।

1. (Step 13 में बना) `add_node_changed_list`: Monitor the currently focused element: मौजूदा एलिमेंट के बदलाव पर बोलना
2. (Step 13 में बना) `node_changed_list_manager`: Focus monitor manager: उन निगरानियों की सूची और हटाना
3. `command_virtual_screen`: Virtual screen: स्क्रीन का वर्चुअल रूप
4. `command_virtual_navigation`: Virtual navigation: वर्चुअल रूप में घूमना
5. `command_all_node`: Show all nodes: सभी नोड दिखाना

## G. बोलकर इनपुट और चैट पढ़ना

बोलकर लिखवाना और आए संदेश अपने आप पढ़ना।

1. (Step 11 में बना) `command_voice_input`: Voice dictation: बोलकर लिखवाना
2. (Step 11 में बना) `voice_input_mode`: continuous voice dictation: लगातार बोलकर लिखवाना
3. `command_auto_list_speak`: Automatically read chat messages: चैट संदेश अपने आप पढ़ना
4. `command_auto_list_speak_history`: Automatic chat messages history: पढ़े संदेशों का इतिहास
5. `command_speak_all_notification`: Read all notifications: सभी सूचनाएँ पढ़ना

## H. पहले आपका फ़ैसला चाहिए

इनमें बाहरी सर्विस, अनुमति या मतलब तय करना ज़रूरी है। फ़ैसले के बिना कोड नहीं लिखूँगा।

1. `command_copy_to_cloud`: Copy to cloud clipboard: क्लाउड क्लिपबोर्ड पर कॉपी। कौन सा सर्वर या अकाउंट?
2. `command_paste_from_cloud`: Paste from cloud clipboard: ऊपर वाले का दूसरा हिस्सा
3. `camera_title`: Jieshuo Camera: कैमरा स्क्रीन। इसमें क्या-क्या हो?
4. `command_current_location`: Current location: वर्तमान जगह बताना। लोकेशन अनुमति चाहिए, कैसे बोलें?
5. `command_left_shortcut`: Left shortcut: बायाँ शॉर्टकट। किस काम का?
6. `command_right_shortcut`: Right shortcut: दायाँ शॉर्टकट। किस काम का?

## समूह H में Step 12 से जुड़े 8 कमांड (फ़ैसला चाहिए)

मूल ऐप के कोरियाई और क्रोएशियाई शब्दों से जो अर्थ मिला, वह साथ में लिखा है।

1. `command_tap_mode`: "टच करने पर फ़ोकस ले जाना" (टच परत चाहिए)
2. `command_up_tap`: "उँगली उठाने पर क्लिक" (टच परत चाहिए)
3. `command_grid_mode`: स्क्रीन पर ग्रिड, जहाँ चाहें टैप (ओवरले चाहिए)
4. `split_menu_title`: बारीक ब्राउज़िंग, टेक्स्ट को टुकड़ों में बाँटकर
5. `split_edit_title`: बारीक एडिटिंग
6. `list_mode_title`: सूची ब्राउज़िंग
7. `command_quick_mode`: तेज़ ब्राउज़िंग
8. `command_supper_mode`: विस्तृत फ़ोकस मोड

इनके बारे में आप बताइए कि कैसे चलें, या मैं अपनी समझ से बना दूँ?

## समूह H में Step 13 से जुड़े 5 कमांड (फ़ैसला चाहिए या कोई जोड़ नहीं मिला)

1. `command_disable_edge_gesture_title`: इस ऐप में एज जेस्चर हैं ही नहीं
2. `switch_multi_finger_gestures`: बहु-उँगली जेस्चर की कोई सेटिंग नहीं मिली
3. `command_incall_mode`: TalkBack की आवाज़ एक्सेसिबिलिटी स्ट्रीम से आती है, ईयरपीस चुनना यहाँ से मुमकिन नहीं
4. `command_pause_a11y_volume`: वह सेटिंग वॉल्यूम का स्तर है, चालू/बंद स्विच नहीं
5. `command_start_log`: कौन सा लॉग लिखना है, यह साफ़ नहीं
