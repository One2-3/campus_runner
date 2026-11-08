# Regenerate the ending subset from the existing, OFL-licensed local source.
# No Windows system font, download, or runtime font dependency is used.
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Push-Location $repoRoot
try {
    & (Join-Path $PSScriptRoot 'GenerateVacationFont.ps1') `
        -OutputStem 'ending-ko' `
        -FaceName 'Campus Ending Korean' `
        -SourcePaths @('core/src/main/java/io/jbnu/test/GraduationEnding.java')
} finally { Pop-Location }
