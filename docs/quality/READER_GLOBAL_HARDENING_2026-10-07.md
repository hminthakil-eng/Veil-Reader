# Veil Reader — برنامهٔ جامع اصلاح و ارتقای کیفیت

تاریخ: ۷ اکتبر ۲۰۲۶. وضعیت این بسته: اصلاح سورس؛ تأیید اجرای اندروید و دستگاه در انتظار.

## ۱. مبنای واقعی و دامنهٔ ادعا

مبنای این اصلاح، آخرین سرشاخهٔ مشاهده‌شدهٔ مسیر صوتی است:
`grand-forge/tts-neural-shared-assets-hardening-v1` روی `b31b7622dc74c0e9d738b785acfd697c733b4651`، درخواست تغییر شمارهٔ ۴۲۶.

این شاخه ادامهٔ مسیر شمارهٔ ۴۲۳ (حالت شنیداری) و ۴۲۴ (آماده‌سازی موتور عصبی) است. مسیر شمارهٔ ۴۲۰ قدیمی‌تر است. «آخرین توسعهٔ صوتی» با «نسخهٔ یکپارچهٔ آمادهٔ انتشار کل اپ» یکسان نیست. شاخهٔ اصلی یکپارچه‌سازی همچنان باید به قرارداد موجود پروژه بازگردد؛ این بستهٔ بازبینی‌شدنی برای ورود به همان مسیر است، نه یک محصول موازی.

شماره‌های ۴۱۸ و ۴۱۹ با این مبنا واگرا هستند. مقایسهٔ تاریخچه، نبود ادغام آن commitها را نشان می‌دهد؛ نبود راه‌حل مشابه در سورس را ثابت نمی‌کند. پیش از ادغام، هم‌پوشانی اصلاحات محل مطالعه و نقطهٔ بازیابی بررسی شود. ادغام کورکورانهٔ هر دو مسیر ممنوع: دو نویسندهٔ موازی برای یک locator ساخته نشود.

رفرنس‌های تصویری پیوست و ممیزی ۲۶ سپتامبر خوانده/مشاهده شدند. ممیزی قبلی یک سند تاریخی است؛ نقص‌هایش باید روی سورس جدید دوباره تأیید شوند. فهرست قابلیت‌های ۵ اکتبر در `docs/audit/REFORGE_EXECUTABLE_FEATURE_INVENTORY_2026-10-05.md` مبنای پوشش است، ولی وضعیت قدیمی صوت، معیار سلامت امروز نیست. PDFهای یادداشت پیوست لایهٔ متن قابل استخراج نداشتند؛ مرور بصری کامل آن‌ها در این بسته انجام نشده است. APK پیوست نصب یا به مبنای فعلی منتسب نشده است.

این نوبت مرور هدفمند سورس و اصلاح واقعی چند مسیر است؛ ممیزی خط‌به‌خط تمام مخزن، آزمون گوشی، یا گواهی کیفیت جهانی نیست. امتیاز درصدی زیبایی یا سلامت بدون شواهد جدید صادر نمی‌شود.

## ۲. اصلاح‌های انجام‌شده در همین بسته

| نقص قابل بازتولید از سورس | تغییر | پوشش جدید و شرط تأیید |
|---|---|---|
| خلاصهٔ صدای شنیداری پس از شکست تطبیق زبان، اولین صدای فهرست را نمایش می‌داد | استفاده از همان سیاست انتخاب صدای آفلاین پخش؛ صدای زبان دیگر جایگزین نمی‌شود | آزمون سیاست صدا + بررسی واقعی catalog دستگاه |
| پیش‌نمایش خودکار اولین صدای فهرست را پخش می‌کرد، نه صدای منتخب سیاست پخش | یکسان‌سازی رتبه‌بندی زبان/گویش/کیفیت؛ ترتیب catalog نتیجه را عوض نمی‌کند | آزمون واحد و آزمون رابط برای لهجهٔ آمریکایی در حضور صدای بریتانیایی با کیفیت بیشتر |
| صدای انتخابی حذف‌شده می‌توانست با صدایی دیگر preview شود | نمایش unavailable و حذف preview جایگزین؛ انتخاب خودکار باید صریح باشد | آزمون صدای ترجیحی حذف‌شده |
| انتخابگر در نبود فارسی، زبان انگلیسی را انتخاب اولیه نشان می‌داد | حفظ زبان کتاب حتی وقتی صدای آن نصب نیست | آزمون زبان ناموجود، فهرست خالی، زبان نامعلوم |
| فهرست صداها برای زبان دارای خط صریح، صدای خط مقابل را هم پیشنهاد می‌داد | اشتراک eligibility میان backend، انتخابگر و preview | آزمون خط ساده/سنتی چینی، صدای شبکه‌ای و نصب‌نشده |
| تنظیمات شنیداری پیشرفته روی یک Box معمولی باز می‌شد | پنجرهٔ مودال واقعی با بستن و بازگشت؛ عرض محدود محتوا | آزمون instrumentation برای وجود پنجره و بازگشت در متن ۲۰۰٪؛ TalkBack واقعی هنوز لازم است |
| محدودیت عرض پس از fillMaxWidth اعمال می‌شد و می‌توانست بی‌اثر شود | ترتیب صحیح محدودیت ۵۲۰/۶۲۰ و پرکردن عرض مجاز | بازبینی تبلت و پنجرهٔ عریض |
| عنوان کتاب/نویسنده/فصل/متن شنیداری در پوستهٔ انگلیسی قواعد خط فارسی را نمی‌گرفت | استفاده از قواعد موجود فونت، پیوستگی حروف و ارتفاع خط | تصویر متن دوزبانه در پوستهٔ انگلیسی |
| برچسب نشانک و عنوان/قطعهٔ نتیجهٔ جست‌وجو در دفترچه همین نشت تایپوگرافی را داشت | تطبیق سبک با متن واقعی، با reuse تابع موجود | بررسی فارسی/عربی، نام بلند و fallback خالی |
| سرعت‌های آماده با اعداد خام و هدف کوچک نمایش داده می‌شدند | قالب‌بندی عدد موجود و حداقل ارتفاع ۴۸ | متن بزرگ، فارسی و لمس |
| خطای شنیداری بدون اعلان تغییر برای فناوری کمکی نمایش می‌یافت | ناحیهٔ اعلان مؤدبانه | خطا با TalkBack، بدون مزاحمت تکراری |
| معیار موتور عصبی می‌توانست نمونهٔ completed بدون متن/صوت، یا نسبت اعلامی خوش‌بینانه را قبول کند | متن و مدت مثبت؛ نسبت واقعی زمان تولید/صوت هم باید پاس شود؛ مقادیر نامتناهی رد می‌شوند | چهار آزمون واحد جدید؛ این معیار به‌تنهایی کیفیت شنیداری را تأیید نمی‌کند |
| درخواست تغییر روی مبنای جدید خارج از فیلتر CI بود | افزودن شاخهٔ مبنا و کامپایل تست‌های instrumentation در workflow موجود | اجرای CI همان سرشاخهٔ اصلاح |

## ۳. منابع استاندارد و کاربرد دقیق

منابع رسمی در ۷ اکتبر بررسی شدند:

- [کیفیت پایهٔ اندروید](https://developer.android.com/docs/quality-guidelines/core-app-quality): قابلیت‌های اصلی، بازیابی، دسترس‌پذیری و اهداف لمس حداقل ۴۸ واحد مستقل از تراکم.
- [کیفیت نمایش تطبیقی](https://developer.android.com/docs/quality-guidelines/adaptive-app-quality): تغییر اندازهٔ پنجره، چندپنجره، تبلت و دستگاه تاشو. معیار بر حسب فضای پنجره است، نه فقط نام دستگاه.
- [دسترس‌پذیری رابط Compose](https://developer.android.com/develop/ui/compose/accessibility): معنا، ترتیب پیمایش، متن قابل تغییر و کنترل سفارشی.
- [پنجرهٔ مودال Compose](https://developer.android.com/develop/ui/compose/components/dialog): استفاده از primitive پنجره برای تنظیمات مودال به‌جای لایهٔ ظاهری.
- [WCAG 2.2](https://www.w3.org/TR/WCAG22/): مبنای ارزیابی تضاد، معنا، ترتیب، reflow و استفادهٔ بدون اشاره‌گر برای محتوای وب/EPUB. استفاده از این معیارها ادعای گواهی انطباق کامل اپ بومی نیست.
- [EPUB Reading Systems 3.3](https://www.w3.org/TR/epub-rs-33/): احترام به ترتیب کتاب، جهت محتوا و قرارداد موتور خواندن.
- [EPUB Accessibility 1.1](https://www.w3.org/TR/epub-a11y/): ساختار و فرادادهٔ دسترس‌پذیری کتاب. اپ نباید دسترس‌پذیریِ محتوایی که ناشر تولید نکرده را جعل کند.
- [EPUB 3.4](https://www.w3.org/TR/epub-34/): نسخهٔ مشاهده‌شده «Candidate Recommendation Draft» مورخ ۲ اکتبر است؛ برای رصد سازگاری آینده، نه ادعای استاندارد نهایی یا مهاجرت عجولانه.
- [EPUB Accessibility 1.2](https://www.w3.org/TR/epub-a11y-12/): نسخهٔ مشاهده‌شده «Candidate Recommendation Draft» مورخ ۱۲ سپتامبر است؛ الزام قطعی انطباق نهایی نیست. تفاوت‌ها در مسیر سازگاری آینده بررسی شوند.

حداقل داخلی: تضاد متن معمولی ۴٫۵ به ۱، متن بزرگ ۳ به ۱ و اجزای معنادار غیرمتنی ۳ به ۱؛ اندازهٔ متن ۱۰۰/۱۳۰/۱۵۰/۲۰۰٪؛ رنگ تنها نشانهٔ حالت نباشد. این‌ها معیارهای ارزیابی هستند، نه نتایج اندازه‌گیری‌شدهٔ فعلی. تغییر فاصله یا فونت نباید قطع متن، هم‌پوشانی یا تغییر ناخواستهٔ محل مطالعه ایجاد کند. استثناهای محتوای ثابت مانند PDF باید با قابلیت صادقانهٔ زوم/پیمایش مشخص شوند.

## ۴. برنامهٔ تمام بخش‌ها با خروج قابل سنجش

این جدول مکمل همان فهرست موجود است و backlog موازی ایجاد نمی‌کند. مسئول در هر ردیف یک نقش کاری است، نه ادعای فعال‌بودن عامل پس‌زمینه. اولویت صفر قبل از انتشار؛ اولویت یک قبل از اعلام تجربهٔ ممتاز؛ اولویت دو توسعهٔ دامنه پس از پایدارشدن هسته.

| بخش و جزئیات پوشش | اولویت / مسئول | کار مشخص | شرط خروج و سناریوی شکست |
|---|---|---|---|
| همگرایی شاخه‌ها و تغییرات باز | صفر / معماری | تطبیق ۴۱۸، ۴۱۹، ۴۲۰، ۴۲۱، ۴۲۳، ۴۲۴، ۴۲۶ با مبنای canonical؛ تصمیم مستند برای هر تفاوت | یک commit مبنا، یک موتور و یک نویسنده برای هر واقعیت؛ هیچ اصلاح منحصربه‌فردی بی‌دلیل حذف نشود |
| بازکردن و بازگشت به کتاب | صفر / مطالعه | تکمیل handshake، لغو open قدیمی، کتاب A→B سریع و بازگردانی route | کتاب جایگزین با callback کتاب قبلی تغییر نکند؛ cold/warm/process restore ثبت شود |
| ذخیرهٔ محل و زمان مطالعه | صفر / داده | بررسی debounce، semantic commit، pause، close، checkpoint، ordering و flush در برابر PRهای باز | force-stop بعد از commit، background سریع، close همزمان؛ هیچ عقب‌گرد locator یا دوباره‌شماری زمان |
| صفحه‌بندی ثابت | صفر / مطالعه | مستقل از animation؛ تأیید بدون حرکت، ابتدا/انتهای کتاب | هر input حداکثر یک جابه‌جایی؛ خاموشی انیمیشن به slide تبدیل نشود |
| ورق کاغذی | صفر / گرافیک و کارایی | بازتأیید انتخاب انجین، corner/edge drag، مقاومت/نور/سایه، لغو و رهاسازی | فیلم first-turn، slow/flick/reverse/cancel در هر دو جهت؛ هیچ destination flash، ورق جعلی یا تغییر locator پیش از commit |
| اسلاید و اسکرول | صفر / مطالعه | مالک جدا و settlement در تغییر mode؛ منع همزمانی paper/slide/scroll | تغییر mode وسط gesture؛ حفظ متن، توقف حرکت قدیمی و یک ناوبری |
| ورودی و chrome | صفر / دسترس‌پذیری | مرکز، tap matrix، hardware keys، selection، focus guide و auto-hide با مالکیت واحد | selection/IME/TalkBack/dialog مانع مخفی‌شدن کنترل لازم شود؛ صفحهٔ انتهایی کلید را نبلعد |
| ظاهر مطالعه | یک / تایپوگرافی | theme، font، weight، scale، line/paragraph/word/letter spacing، margin، columns، publisher styles، ligature، hyphenation و dark images | تک‌تک کنترل‌ها روی محتوای واقعی اثر کند؛ live preview→cancel→apply→restart؛ تغییرهای نامناسب برای خط پیوسته توضیح/محدود شوند |
| روشنایی و حس کاغذ | یک / مطالعه | محدودهٔ system/custom، بازگردانی session، texture کم‌مزاحمت و OLED | خروج از Reader روشنایی سیستم را خراب نکند؛ متن بدون grain هم خوانا؛ مطالعهٔ طولانی خسته‌کننده نشود |
| فهرست فصل و بازگشت قبلی | یک / ناوبری | فصل عمیق، nested TOC، previous location و boundary feedback | بازگشت به همان passage پس از جست‌وجو/یادداشت/فصل؛ نه تخمین درصد |
| انتخاب، نشانک، هایلایت و یادداشت | صفر / داده و مطالعه | handleهای واقعی، quote دقیق، draft پایدار، ویرایش/حذف و رنگ دارای نشانهٔ غیررنگی | rotation/process death هنگام نوشتن؛ برگشت دقیق به متن فارسی/انگلیسی؛ حذف رکورد اشتباه ناممکن |
| جست‌وجوی داخل کتاب | یک / جست‌وجو | لغو query قدیمی، نتایج bounded، snippet خط‌آگاه و focus/IME | query سریع و کتاب عوض‌شده نتیجهٔ stale ندهند؛ cancel بازگشت‌پذیر و نتیجهٔ خالی واضح |
| PDF پایه | صفر / PDF | render، صفحه/scroll، zoom، fit width/reset، rotation و locator migration | سند landscape، اسکن، متن، رمزدار/خراب و بزرگ؛ pinch همزمان ورق نزند؛ chrome در zoom مزاحم نشود |
| PDF پیشرفته | دو / PDF | author/save annotation، forms، OCR و two-page؛ pilot پیش از default | فایل خروجی دوباره در viewer مستقل باز شود؛ OCR و صدای PDF بدون متن معتبر عرضه نشوند |
| صدای سیستم و حالت شنیداری | صفر / صوت | انتخاب voice، preview واقعی، Play/Pause/Stop، semantic prev/next، سرعت و pitch | نمونهٔ صدا با پخش سازگار؛ زبان ناموجود پیام روشن؛ Next در PREPARING دقیقاً یک‌بار؛ موقعیت خواندن بصری خودکار تغییر نکند |
| صوت پس‌زمینه | صفر / صوت و پلتفرم | MediaSession، notification، focus/noisy، Bluetooth، headset، lock screen و process restore | تماس/قطع هدفون/Play هنگام restore/مرگ service؛ به‌دست‌آوردن focus لازم و عدم autoplay غیرمنتظره |
| sleep timer و read-along | یک / صوت | deadline پایدار، لغو، پایان زمان و highlight semantic بدون پرش اجباری | خاموشی صفحه و restore؛ تایمر منقضی پخش را دوباره فعال نکند؛ underline/scroll قابل خاموشی |
| مدل عصبی | یک / صوت و کارایی | package validation، staging، rollback، assets، انتخاب مدل و benchmark واقعی؛ native runtime هنوز جداگانه اثبات شود | SHA/package معتبر، لغو/کمبود فضا/مدل خراب؛ مدت صوت مثبت و نسبت واقعی؛ آزمون گوش، حافظه، گرما، باتری و مجوز مدل |
| آستانه و ادامهٔ مطالعه | یک / طراحی | Continue قبل از lore؛ کاهش ارتفاع تکرارشوندهٔ hero؛ recent، pulse و empty | در اندازهٔ کوچک/متن ۲۰۰٪ اقدام ادامه گم نشود؛ tap-to-text سنجیده و مسیر واقعی باشد |
| کتابخانهٔ سه‌حالته | یک / طراحی و جست‌وجو | Gallery/Shelves/Index، state filters، refine، sort، search، collections و series | اولین کتاب سریع دیده شود؛ query/filter حفظ شود؛ ۱هزار/۱۰هزار کتاب، بدون توقف main thread |
| جزئیات کتاب و metadata | یک / طراحی | جلد/نام/نویسنده/Continue بالاتر از lore؛ حقایق واقعی، شرح جمع‌شونده | عنوان بلند، جلد خراب، نویسندهٔ خالی، چند collection، series index نامعتبر؛ دادهٔ ساختگی نمایش نیابد |
| ورود فایل و duplicate | صفر / داده | app-private copy، fingerprint، cancellation، recovery و relink | منبع مجوز ازدست‌رفته، فایل تکراری، فضای ناکافی، سند خراب؛ کتاب نیمه‌واردشده باقی نماند |
| فرمت‌های بیشتر | دو / قالب | TXT/HTML/Markdown، سپس FB2/MOBI/AZW/DjVu فقط پس از بررسی parser/مجوز و معیار | هر قالب سطح واقعی fidelity/search/annotation/backup داشته باشد؛ unsupported شفاف بماند |
| آرشیو و دفترچهٔ سراسری | یک / طراحی و داده | Notes/Highlights/Bookmarks/Echoes/Capsules، حذف شمارش تکراری و taxonomy روشن | رکورد درست، quote/note متمایز، متن دوزبانه، query و reset؛ تاریخ/محتوا گم نشود |
| حافظه و کپسول تاریخی | یک / داده | completion cycle و snapshot پایدار؛ revisit فقط با مشاهدهٔ passage | بازخوانی کپسول قبلی را تغییر ندهد؛ تاریخ نامعلوم جعل نشود؛ backup roundtrip |
| خروجی یادداشت و lookup | دو / دانش | Markdown/HTML/JSON export؛ lookup داخلی با PROCESS_TEXT fallback، ترجمه/وب اختیاری | escaping متن، رکورد بدون quote، offline، نبود handler و cancel؛ ارسال متن فقط با اقدام آگاهانه |
| تنظیمات عمومی | یک / طراحی | progressive disclosure برای Reader/Accessibility/Library & Data/Experience/World/About | تنظیم در ≤۲ سطح منطقی پیدا شود؛ scope global/per-book روشن؛ reset پیامد مشخص و انتخاب صریح |
| پوسته و ناوبری | یک / معماری و طراحی | hierarchy مطالعه/کتابخانه/دفترچه/بیشتر؛ جهان از ورودی زمینه‌ای؛ back/deep link | route restore و predictive-back؛ tab پنهان مسیر dead-end نسازد؛ هیچ دسترسی به مطالعه پشت بازی |
| فارسی، عربی و متن دوزبانه | یک / تایپوگرافی | connected script، اعداد محلی، متن خارجی، bidi و جهت کتاب مستقل از پوسته | عنوان/نشانک/quote فارسی در UI انگلیسی و برعکس؛ URL/عدد/Latin و selection بدون خرابی |
| دسترس‌پذیری همهٔ قلمروها | صفر / دسترس‌پذیری | semantics، focus، modal isolation، contrast، large text، keyboard/Switch Access/reduced motion | تمام اقدام‌ها بدون gesture اختصاصی؛ خطا قابل اعلام؛ focus پس از dialog به جای درست برگردد |
| نمایش تطبیقی | یک / طراحی و معماری | phone portrait/landscape، split-screen، tablet/fold، IME و insets | resize بدون loss؛ ۲۴۰/۳۲۰/۴۱۲/۶۰۰/۸۴۰+ عرض و متن بزرگ؛ حد عرض واقعاً اعمال شود |
| قلعه، مسیر، آیین و خلوتگاه | یک سپس دو / جهان و داده | spatial hierarchy، rank/unlock/relic/treasury، skip ceremony و کاهش حرکت | مطالعه مستقل؛ reward دائمی و exactly-once؛ حذف/restore داده تاریخ جعلی نسازد؛ کاربر بازی را خاموش کند |
| رصدخانه و پروفایل | یک / جهان و داده | graph واقعی، history، discovery پایدار و آمار منسجم | کتابخانهٔ بزرگ؛ روابط rule-based به‌عنوان AI معرفی نشوند؛ کشف کسب‌شده با افت streak محو نشود |
| مانگا و کمیک محلی | دو / مانگا | CBZ، catalog، gesture ownership، zoom/subsampling، progress و backup | تصویر خیلی بزرگ، chapter عوض‌شده، process death؛ شبکهٔ live همچنان ادعای آماده‌بودن نگیرد |
| خطا، خالی، loading و recovery | صفر / طراحی و معماری | حالت مستقل برای هر عملیات؛ اقدام retry/relink/cancel و علت قابل فهم | نه spinner بی‌پایان، نه خطای خام؛ retry عملیات تکراری مخرب نسازد |
| backup و restore | صفر / داده | schema migration، staged restore، preview، rollback و checksum | roundtrip کتاب/موقعیت/یادداشت/جهان/CBZ؛ traversal، archive bomb و کمبود فضا؛ دادهٔ قبلی حفظ شود |
| sync و privacy | دو / داده و امنیت | optional account، conflict contract، idempotency و local-first؛ scope واقعی | دو دستگاه offline→reconnect؛ حذف/تعارض روشن؛ متن کتاب یا صدا بدون رضایت به شبکه نرود |
| کارایی و انرژی | صفر سپس یک / کارایی | startup/first turn/jank، large library، native audio، leaks/StrictMode و profiles | زمان فریم در نرخ واقعی دستگاه، memory plateau جلسهٔ بلند، battery/thermal و regression baseline |
| بسته و انتشار | صفر / انتشار | R8/shrink، ABI، profile، build facts، dependency provenance و source→package SHA | همان SHA در گزارش، binary و evidence؛ feature gateهای اثبات‌نشده باز نشوند؛ حجم فقط با ارزش و بودجه توجیه شود |

## ۵. ترتیب اجرا و وابستگی‌ها

۱. بستن همین اصلاحات صوتی با آزمون واحد/کامپایل instrumentation و بررسی دستگاه؛ رفع هر خطای واقعی CI قبل از بازطراحی بیشتر.
۲. همگرایی ۴۱۸/۴۱۹ و قطعیت ذخیرهٔ مطالعه؛ سپس بازتأیید چهار mode مستقل و PDF zoom.
۳. کوتاه‌کردن مسیر ادامهٔ مطالعه، کنترل‌های کتابخانه و تنظیمات؛ سپس Archive/Notebook و کل متن دوزبانه.
۴. ماتریس سراسری دسترس‌پذیری، تغییر اندازه و lifecycle؛ اصلاح‌ها به همان مالک موجود برگردند.
۵. جهان/مانگا/قابلیت‌های جدید پس از رفع هستهٔ مطالعه؛ native TTS تنها بعد از اثبات صوت واقعی.
۶. انتشار فقط از خط canonical همگرا با شواهد همان commit.

هر مرحله: بازتولید مشکل → شاهد سورس/اجرا → اصلاح حداقلیِ مالک اصلی → regression معنادار → آزمون دستگاه مربوط → ثبت باقیمانده‌ها. کارهای کم‌خطر بصری به تست‌های مصنوعی که فقط implementation را تکرار می‌کنند نیاز ندارند؛ با تصویر و سناریوی واقعی سنجیده شوند.

## ۶. ماتریس آزمون مشترک

- محتوا: EPUB انگلیسی بلند، فارسی/عربی، mixed-script، illustration-heavy، CSS ناشر، fixed-layout؛ PDF متنی/اسکن/بزرگ/چرخیده؛ CBZ با تصویر بزرگ. از کتاب‌های پیوست برای آزمون استفاده شود، نه انتشار محتوای آن‌ها در گزارش یا fixture عمومی.
- وضعیت: نصب تازه، کاربر بازگشته، بدون کتاب/جلد/metadata، کتاب خراب، صدای ناموجود، offline، فضای کم، backup قدیمی، catalog در حال load.
- وقفه: rotation، resize، split screen، IME، Home، lock، foreground/background سریع، service recreation و force-stop پس از commit.
- ورودی: touch، slow drag/flick/reverse، keyboard، volume keys، TalkBack، Switch Access، Bluetooth/headset و تماس.
- نمایش: روشن/تاریک/OLED/sepia، contrast بالا، حرکت کم، متن ۱۰۰–۲۰۰٪، دو جهت پوسته و دو جهت کتاب، تلفن/تبلت/تاشو.
- شواهد: commit کامل، binary hash، device/OS/refresh rate، scenario، expected/actual، تصویر/فیلم، اندازه‌گیری و شکست‌های باز.

اهداف داخلی پیشنهادی که هنوز اندازه‌گیری نشده‌اند: مسیر resume گرم تا متن ≤۱ ثانیه روی دستگاه مرجع؛ جست‌وجوی metadata روی ۱۰هزار کتاب با تأخیر صدک ۹۵ ≤۳۰۰ میلی‌ثانیه؛ اولین صوت ≤۱۲۰۰ میلی‌ثانیه و نسبت تولید/مدت صوت ≤۰٫۸۵. این هدف‌ها باید با fixture و سخت‌افزار مشخص تثبیت شوند. سقف زمان فریم تابع refresh واقعی است: حدود ۱۶٫۶۷/۱۱٫۱۱/۸٫۳۳ میلی‌ثانیه برای ۶۰/۹۰/۱۲۰ هرتز، همراه با سنجش missed frames، نه فقط average.

برای خوانایی و راحتی، جلسهٔ واقعی ۳۰ و ۹۰ دقیقه لازم است؛ capture زیبای یک صفحه جانشین آن نیست. معیار تشخیص نقص کاغذ: absence of real deformation، flicker، destination flash، لق‌شدن locator، توقف اولین ورق و خستگی حرکتی؛ هرکدام انتشار آن قابلیت را متوقف می‌کند.

## ۷. شواهد این نوبت و محدودیت

- ۳۹ بررسی سیاست مطالعه: پاس.
- قرارداد سورس کاغذ canonical: پاس.
- چهار آزمون محافظ کاغذ: پاس.
- آزمون‌های parser داشبورد کیفیت: پاس.
- بررسی whitespace تغییرها: پاس.
- ۱۲ آزمون واحد regression جدید و ۲ آزمون instrumentation جدید نوشته شد؛ اجرای آن‌ها هنوز تأیید نشده است.
- تلاش `:app:testDebugUnitTest` پیش از شروع کامپایل، هنگام دریافت Gradle به خطای «Network is unreachable» رسید. ابزار اندروید آمادهٔ اجرای دستگاه در این محیط نیست. نتیجه شکست محیط است، نه نتیجهٔ اجرای تست‌ها.
- تطبیق پیکسلی اجرای فعلی، instrumentation، benchmark native، نصب APK و مطالعهٔ واقعی انجام نشده‌اند. فایل `quality/physical-device-status.json` همچنان UNVERIFIED باقی می‌ماند.

برای ختم هر ردیف جدول، وجود کد کافی نیست: رفتار کامل + دادهٔ درست + هویت تصویری + دسترس‌پذیری + شواهد همان SHA لازم است. پیشرفت این نوبت «اصلاح انجام‌شده در سورس، در انتظار تأیید» است.


## P0: Paper activation / frozen input (2026-10-07)

- User reports the supplied debug APK neither turns a page nor displays the Paper debug label. APK includes the GPU engine and runtime debug rollout; this does not establish a GPU rendering failure.
- Avoid re-submitting identical Readium preferences on Veil-only Paper/Slide/None switches. Compare mapped EPUB/PDF preferences, settle outstanding previews, then publish the visual/input mode together.
- Bound preview completion (2 seconds per engine), optional precise-locator queries (1 second), and renderer application (6 seconds). Always release the input lock, including lifecycle cancellation before preference submission.
- Preserve acquired-buffer acknowledgement and one navigation owner. No fake curl or slide substitution.
- Regression coverage: Paper activation skips identical renderer preferences; scroll, typography, theme and spread changes still submit. Existing initial-attach, preview ownership and canonical Paper tests remain required.
- Debug APK is uploaded after successful PR CI for device validation. Device acceptance remains open: reflowable EPUB, Paper activation, forward/back turns, cancellation, rapid mode changes, position continuity, background/resume. No on-device success is claimed.
