import re, pathlib

ROOT = pathlib.Path(r"D:\CodexMigration\Workspaces\2026-11-08\files-pasted-by-the-user-steam\outputs\SteamLite\app\src\main\java\org\steamlite")
MINE = ["SteamUi.kt","UiGlyph.kt","Components.kt","SteamImage.kt","MainActivity.kt",
        "StoreScreen.kt","WishlistScreen.kt","ChatHubScreen.kt","ProfileSummary.kt",
        "GameDetailsDialog.kt","NotificationSettings.kt"]

defs = set()
for f in ROOT.glob("*.kt"):
    for m in re.finditer(r'\bfun\s+(?:<[^>]+>\s*)?([A-Za-z_][\w]*)', f.read_text(encoding="utf-8")):
        defs.add(m.group(1))

# project types / vals that get called or constructed
vals = set()
for f in ROOT.glob("*.kt"):
    for m in re.finditer(r'\b(?:val|class|object|enum class|data class)\s+([A-Za-z_][\w]*)', f.read_text(encoding="utf-8")):
        vals.add(m.group(1))

WHITELIST = set("""
Text Row Column Box Spacer Button Card Surface Icon Image Canvas Path Brush Offset Size CornerRadius
Color SolidColor StrokeCap StrokeJoin Stroke DrawScope Modifier Dp Int Float Long Double String Boolean
List MutableList Set Map Pair Triple Array IntArray Unit Any Nothing Void Byte Short Char
TextStyle FontWeight TextDecoration TextAlign TextOverflow ImeAction KeyboardType KeyboardOptions
KeyboardActions KeyboardActionsScope PasswordVisualTransformation VisualTransformation
RoundedCornerShape CircleShape CutCornerShape RectangleShape
Arrangement Alignment ContentScale ContentScaleAsync Painter
LazyColumn LazyRow LazyVerticalGrid GridCells LazyListState rememberLazyListState
Scaffold SnackbarHost SnackbarHostState SnackbarDuration SnackbarResult SnackbarData
ModalBottomSheet ExperimentalMaterial3Api Checkbox CheckboxDefaults ButtonDefaults
MaterialTheme Typography TextStyle darkColorScheme ColorScheme
BasicTextField TextField OutlinedTextField
MaterialTheme TextButton FilterChip NavigationBar NavigationBarItem AlertDialog DropdownMenu
DropdownMenuItem LinearProgressIndicator CircularProgressIndicator HorizontalDivider
LocalContext LocalContentColor LocalLifecycleOwner LocalDensity
AsyncImage Coil ImageRequest
JSONObject JSONArray Html Toast Intent Uri Bundle Context Application
SimpleDateFormat Date Locale Calendar NumberFormat
LaunchedEffect DisposableEffect remember rememberSaveable mutableStateOf mutableIntStateOf
collectAsState collectAsStateWithLifecycle deriveStateOf
BackHandler WindowInsets NavigationBarDefaults
Lifecycle RESUMED repeatOnLifecycle
isSuccess getOrDefault runCatching require check error println
Math Integer System Thread
Message FriendRow RecentSession Game SteamGroup GroupChannel OwnedGame LibraryStats
Tone Presence BubbleData BubbleRow Glyph ConnectionRow
""".split())

unknown = {}
for name in MINE:
    f = ROOT / name
    src = f.read_text(encoding="utf-8")
    # strip strings
    src = re.sub(r'""".*?"""', '""', src, flags=re.S)
    src = re.sub(r'"(?:\\.|[^"\\])*"', '""', src)
    for m in re.finditer(r'(?<![\w.])([A-Z][A-Za-z0-9_]*)\s*[({]', src):
        sym = m.group(1)
        if sym in defs or sym in vals or sym in WHITELIST:
            continue
        line = src.count('\n', 0, m.start()) + 1
        unknown.setdefault(sym, set()).add(f"{name}:{line}")

for sym in sorted(unknown):
    sites = sorted(unknown[sym])[:4]
    print(f"{sym:28} {', '.join(sites)}")
print("-" * 60)
print(len(unknown), "unresolved capitalised symbols (may be enum/members — review)")
