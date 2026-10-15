# Batch 7: install real cat voice + real purring, all downloaded from Wikimedia Commons.
# Run in your MeowAlarm project root BEFORE git add/commit/push.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$targets = @(
  @{Name='Realistic meow'; Url='https://commons.wikimedia.org/wiki/Special:Redirect/file/Meow_of_a_pleading_cat.oga'; Relative='app\src\main\res\raw\real_cat_meow.ogg'},
  @{Name='Cute meow (real young cat)'; Url='https://commons.wikimedia.org/wiki/Special:Redirect/file/Maullido_de_gata_hembra_joven.ogg'; Relative='app\src\main\res\raw\cute_cat_v7.ogg'},
  @{Name='Real cat purr'; Url='https://commons.wikimedia.org/wiki/Special:Redirect/file/Purring_cat_bertie.ogg'; Relative='app\src\main\assets\cozy2d\purr_real.ogg'},
  @{Name='Human sigh'; Url='https://commons.wikimedia.org/wiki/Special:Redirect/file/Hm_and_sigh.ogg'; Relative='app\src\main\assets\cozy2d\sigh_real.ogg'},
  @{Name='Human yawn'; Url='https://commons.wikimedia.org/wiki/Special:Redirect/file/Yawning_1.ogg'; Relative='app\src\main\assets\cozy2d\yawn_real.ogg'}
)
foreach ($item in $targets) {
  $target = Join-Path $root $item.Relative
  $parent = Split-Path $target
  New-Item -ItemType Directory -Path $parent -Force | Out-Null
  $temp = "$target.download"
  Write-Host "Downloading $($item.Name) ..."
  try {
    Invoke-WebRequest -Uri $item.Url -OutFile $temp -MaximumRedirection 10 -UseBasicParsing -ErrorAction Stop
    $bytes = [System.IO.File]::ReadAllBytes($temp)
    if ($bytes.Length -lt 1000 -or [Text.Encoding]::ASCII.GetString($bytes,0,4) -ne 'OggS') {
      throw "Downloaded file is not valid Ogg audio."
    }
    Move-Item -Path $temp -Destination $target -Force
    Write-Host "SUCCESS: $($item.Name) ($($bytes.Length) bytes)" -ForegroundColor Green
  } catch {
    if (Test-Path $temp) {Remove-Item $temp -Force}
    Write-Warning "Could not download $($item.Name): $($_.Exception.Message)"
    Write-Warning "Download it manually from $($item.Url) and save as $target"
  }
}
Write-Host 'Done. Review AUDIO_CREDITS.md for sound sources and licenses.'
