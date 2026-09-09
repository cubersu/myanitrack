$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path $PSScriptRoot -Parent
$target = Join-Path $root 'distribution/store-assets'
New-Item -ItemType Directory -Force $target | Out-Null
$icon = [Drawing.Bitmap]::new(512,512)
$canvas = [Drawing.Graphics]::FromImage($icon)
$canvas.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
$canvas.Clear([Drawing.ColorTranslator]::FromHtml('#2E51A2'))
$points = [Drawing.PointF[]]@([Drawing.PointF]::new(170.67,161.19),[Drawing.PointF]::new(170.67,350.81),[Drawing.PointF]::new(331.85,256))
$canvas.FillPolygon([Drawing.Brushes]::White,$points)
$canvas.FillRectangle([Drawing.Brushes]::White,350.81,161.19,37.93,189.62)
$icon.Save((Join-Path $target 'icon-512.png'),[Drawing.Imaging.ImageFormat]::Png)
$canvas.Dispose()
$banner = [Drawing.Bitmap]::new(1024,500)
$canvas = [Drawing.Graphics]::FromImage($banner)
$canvas.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
$canvas.TextRenderingHint = [Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$canvas.Clear([Drawing.ColorTranslator]::FromHtml('#F5F6F8'))
$canvas.DrawImage($icon,55,130,240,240)
$titleFont = [Drawing.Font]::new('Segoe UI',46,[Drawing.FontStyle]::Bold)
$bodyFont = [Drawing.Font]::new('Segoe UI',21)
$brush = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#1C2530'))
$canvas.DrawString('MyAniTrack',$titleFont,$brush,335,157)
$canvas.DrawString('Anime & manga tracker',$bodyFont,$brush,340,248)
$banner.Save((Join-Path $target 'feature-1024x500.png'),[Drawing.Imaging.ImageFormat]::Png)
$titleFont.Dispose(); $bodyFont.Dispose(); $brush.Dispose(); $canvas.Dispose(); $banner.Dispose(); $icon.Dispose()
