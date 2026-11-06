# Regenerate the small bitmap atlas used by the Korean ending copy. No TTF is shipped.
# Requires Windows with Malgun Gothic. Run from the repository root.
Add-Type -AssemblyName System.Drawing
$characters = New-Object 'System.Collections.Generic.SortedSet[char]'
32..126 | ForEach-Object { [void]$characters.Add([char]$_) }
$source = Get-Content -Encoding UTF8 'core/src/main/java/io/jbnu/test/GraduationEnding.java' -Raw
foreach ($match in [regex]::Matches($source, '"([^"\r\n]*)"')) {
    foreach ($character in $match.Groups[1].Value.ToCharArray()) { [void]$characters.Add($character) }
}
$fontCollection = New-Object System.Drawing.Text.PrivateFontCollection
$fontCollection.AddFontFile((Join-Path $env:WINDIR 'Fonts/malgun.ttf'))
$font = New-Object System.Drawing.Font($fontCollection.Families[0], 24, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$rows = [int][Math]::Ceiling($characters.Count / 16.0)
$bitmap = New-Object System.Drawing.Bitmap(512, ($rows * 36), [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$format = [System.Drawing.StringFormat]::GenericTypographic
$lines = New-Object 'System.Collections.Generic.List[string]'
$lines.Add('info face="Ending Korean Bitmap" size=24 bold=0 italic=0 charset="" unicode=1 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=0,0')
$lines.Add("common lineHeight=36 base=28 scaleW=512 scaleH=$($rows * 36) pages=1 packed=0")
$lines.Add('page id=0 file="ending-ko.png"')
$lines.Add("chars count=$($characters.Count)")
$index = 0
foreach ($character in $characters) {
    $x = ($index % 16) * 32
    $y = [int][Math]::Floor($index / 16.0) * 36
    $text = [string]$character
    $advance = [int][Math]::Ceiling($graphics.MeasureString($text, $font, [System.Drawing.PointF]::Empty, $format).Width)
    $graphics.DrawString($text, $font, [System.Drawing.Brushes]::White, [single]($x + 2), [single]($y + 2), $format)
    $lines.Add("char id=$([int]$character) x=$x y=$y width=32 height=36 xoffset=-2 yoffset=0 xadvance=$advance page=0 chnl=15")
    $index++
}
New-Item -ItemType Directory -Force 'assets/fonts' | Out-Null
$bitmap.Save((Join-Path (Get-Location) 'assets/fonts/ending-ko.png'), [System.Drawing.Imaging.ImageFormat]::Png)
[System.IO.File]::WriteAllLines((Join-Path (Get-Location) 'assets/fonts/ending-ko.fnt'), $lines, (New-Object System.Text.UTF8Encoding($false)))
$graphics.Dispose()
$bitmap.Dispose()
$font.Dispose()
$fontCollection.Dispose()
Write-Output "Generated ending bitmap font: $($characters.Count) glyphs"
