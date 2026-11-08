param(
    [string]$OutputStem = "vacation-ko",
    [string]$FaceName = "Campus Vacation Korean",
    [string[]]$SourcePaths = @("core/src/main/java/io/jbnu/test/VacationMinigameRenderer.java", "core/src/main/java/io/jbnu/test/VacationUiTheme.java")
)
# Regenerate the minigame-only Korean subset, using the OFL source and notice in tools/fonts.
# No network, FreeType runtime dependency, or change to the existing ending atlas.
Add-Type -AssemblyName System.Drawing
$characters = New-Object 'System.Collections.Generic.SortedSet[char]'
32..126 | ForEach-Object { [void]$characters.Add([char]$_) }
foreach ($path in $SourcePaths) {
    $source = Get-Content -Encoding UTF8 $path -Raw
    foreach ($match in [regex]::Matches($source, '"([^"\r\n]*)"')) {
        foreach ($character in $match.Groups[1].Value.ToCharArray()) { [void]$characters.Add($character) }
    }
}
$collection = New-Object System.Drawing.Text.PrivateFontCollection
$collection.AddFontFile((Join-Path (Get-Location) 'tools/fonts/NanumGothic-Bold.ttf'))
$font = New-Object System.Drawing.Font($collection.Families[0], 24, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$rows = [int][Math]::Ceiling($characters.Count / 16.0)
$bitmap = New-Object System.Drawing.Bitmap(512, ($rows * 36), [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$format = [System.Drawing.StringFormat]::GenericTypographic
$lines = New-Object 'System.Collections.Generic.List[string]'
# Renamed derivative as required by OFL reserved font names.
$lines.Add(('info face="{0}" size=24 bold=1 italic=0 charset="" unicode=1 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=0,0' -f $FaceName))
$lines.Add("common lineHeight=36 base=28 scaleW=512 scaleH=$($rows * 36) pages=1 packed=0")
$lines.Add(('page id=0 file="{0}.png"' -f $OutputStem))
$lines.Add("chars count=$($characters.Count)")
$index = 0
try {
    foreach ($character in $characters) {
        $x = ($index % 16) * 32
        $y = [int][Math]::Floor($index / 16.0) * 36
        $advance = [int][Math]::Ceiling($graphics.MeasureString([string]$character, $font, [System.Drawing.PointF]::Empty, $format).Width)
        if ($character -eq ' ') { $advance = 8 }
        else { $graphics.DrawString([string]$character, $font, [System.Drawing.Brushes]::White, [single]($x + 2), [single]($y + 2), $format) }
        $lines.Add("char id=$([int]$character) x=$x y=$y width=32 height=36 xoffset=-2 yoffset=0 xadvance=$advance page=0 chnl=15")
        $index++
    }
    $bitmap.Save((Join-Path (Get-Location) ("assets/fonts/" + $OutputStem + ".png")), [System.Drawing.Imaging.ImageFormat]::Png)
    [System.IO.File]::WriteAllLines((Join-Path (Get-Location) ("assets/fonts/" + $OutputStem + ".fnt")), $lines, (New-Object System.Text.UTF8Encoding($false)))
    Copy-Item -LiteralPath 'tools/fonts/OFL.txt' -Destination ("assets/fonts/" + $OutputStem + "-OFL.txt")
} finally {
    $graphics.Dispose(); $bitmap.Dispose(); $font.Dispose(); $collection.Dispose()
}
Write-Output "Generated $FaceName ($OutputStem): $($characters.Count) glyphs, 512 x $($rows * 36)"
