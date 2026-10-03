# Veil PDF fixture

`veil-links-annotations.pdf` is an independently generated ASCII PDF 1.4, not competitor content.
It contains three pages of original text using the PDF built-in Helvetica font, one internal link
from document page 1 to document page 3, and one highlight annotation with an explicit yellow
appearance stream. No font asset is packaged. The explicit appearance makes annotation-flag
rendering deterministic without relying on a PDF viewer to synthesize missing appearances.

The native instrumentation test validates resolved Pdfium link destination indices and rendering
with/without existing annotations. Reader UI, actual RTL navigator mapping, pan/zoom, previous
location restoration, rotation and process death still require the Reader device matrix.
