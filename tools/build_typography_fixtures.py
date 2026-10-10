#!/usr/bin/env python3
"""Build original, deterministic EPUB 3 typography/RTL QA publications (no remote assets)."""
from pathlib import Path
from html import escape
import struct
import zlib
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PROSE = 'A reader opened the archive at dawn. Clear type left room for thought, and every pause belonged to the sentence. '
FA = 'صبح آرامی بود. خواننده کتاب را گشود و با دقت به جمله‌ها نگاه کرد. فاصلهٔ واژه‌ها و شکل حروف باید روشن و خوانا باشد. '
AR = 'فتح القارئ الكتاب في صباح هادئ. ينبغي أن تبقى الحروف متصلة وأن يكون اتجاه القراءة واضحا. '
STYLE = 'body{font-family:serif}p{margin:0 0 1em}img{max-width:100%;height:auto}.poem{white-space:pre-line}table{border-collapse:collapse}th,td{border:1px solid;padding:.3em}'


def png():
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
    width, height = 1024, 512
    pixels = b''.join(b'\0' + bytes(40 if (x // 64 + y // 64) % 2 else 220 for x in range(width)) for y in range(height))
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 0, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(pixels)) + chunk(b'IEND', b'')


def write_epub(name, language, progression, chapters):
    records = [('mimetype', b'application/epub+zip')]
    records.append(('META-INF/container.xml', b'<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="EPUB/package.opf" media-type="application/oebps-package+xml"/></rootfiles></container>'))
    manifest = '<item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/><item id="image" href="pattern.png" media-type="image/png"/>'
    spine = ''
    links = ''
    for i, (title, lang, direction, body, properties) in enumerate(chapters, 1):
        properties = f' properties="{properties}"' if properties else ''
        manifest += f'<item id="c{i}" href="c{i}.xhtml" media-type="application/xhtml+xml"{properties}/>'
        spine += f'<itemref idref="c{i}"/>'
        links += f'<li><a href="c{i}.xhtml">{escape(title)}</a></li>'
        doc = f'<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" lang="{lang}" xml:lang="{lang}" dir="{direction}"><head><title>{escape(title)}</title><style>{STYLE}</style></head><body><h1>{escape(title)}</h1>{body}</body></html>'
        records.append((f'EPUB/c{i}.xhtml', doc.encode()))
    opf = f'<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="id"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="id">urn:veil:qa:{name}:1</dc:identifier><dc:title>Veil Typography QA {progression.upper()}</dc:title><dc:language>{language}</dc:language><dc:creator>Veil QA</dc:creator><meta property="dcterms:modified">2026-10-08T00:00:00Z</meta></metadata><manifest>{manifest}</manifest><spine page-progression-direction="{progression}">{spine}</spine></package>'
    nav = f'<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" lang="{language}" xml:lang="{language}"><head><title>Contents</title></head><body><nav epub:type="toc"><h1>Contents</h1><ol>{links}</ol></nav></body></html>'
    records.extend([('EPUB/package.opf', opf.encode()), ('EPUB/nav.xhtml', nav.encode()), ('EPUB/pattern.png', png())])
    target = ROOT / 'qa' / 'fixtures' / f'{name}.epub'
    with zipfile.ZipFile(target, 'w') as archive:
        for index, (path, data) in enumerate(records):
            info = zipfile.ZipInfo(path, date_time=(2026, 10, 8, 0, 0, 0))
            info.compress_type = zipfile.ZIP_STORED if index == 0 else zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, data)
    print(target.relative_to(ROOT))


def main():
    table = '<table><caption>Wide reference table</caption><thead><tr>' + ''.join(f'<th scope="col">Column {i}</th>' for i in range(1, 13)) + '</tr></thead><tbody>' + ''.join('<tr>' + ''.join(f'<td>Row {r}, value {c}</td>' for c in range(1, 13)) + '</tr>' for r in range(1, 9)) + '</tbody></table>'
    chapters = [
        ('English prose', 'en', 'ltr', ''.join(f'<p id="p{i}">{PROSE * 5}</p>' for i in range(40)), ''),
        ('Poetry and dialogue', 'en', 'ltr', '<p class="poem">One line holds the morning.\nAnother holds the rain.\n\nKeep this pause between them.</p><p>“Did you find the passage?” she asked.</p><p>“Yes,” he said. “Its place was saved.”</p>', ''),
        ('Nested formatting and symbols', 'en', 'ltr', '<h2>A second heading</h2><p><strong>Strong text with <em>nested emphasis</em></strong>, <abbr title="for example">e.g.</abbr>, office affinity, café, naïve, a\u0301, ∑ ∞ → and 😀.</p><blockquote><p>Quoted thought, with <sup>superscript</sup> and <sub>subscript</sub>.</p></blockquote><ol><li>First item<ul><li>Nested item</li></ul></li><li>Second item</li></ol>', ''),
        ('Long paragraph', 'en', 'ltr', f'<p>{PROSE * 350}</p>', ''),
        ('Image and table', 'en', 'ltr', '<figure><img src="pattern.png" width="1024" height="512" alt="Alternating light and dark squares for zoom and contrast checks"/><figcaption>Original QA pattern; check fit, zoom and dismissal.</figcaption></figure>' + table, ''),
        ('Footnote and mathematics', 'en', 'ltr', '<p id="origin">A short reference<a epub:type="noteref" href="#note" role="doc-noteref">1</a> preserves the reading context.</p><aside epub:type="footnote" id="note"><p>Original footnote text. <a href="#origin">Return to reference</a></p></aside><p>Where supported:</p><math xmlns="http://www.w3.org/1998/Math/MathML"><mrow><msup><mi>x</mi><mn>2</mn></msup><mo>+</mo><msup><mi>y</mi><mn>2</mn></msup><mo>=</mo><msup><mi>z</mi><mn>2</mn></msup></mrow></math>', 'mathml'),
        ('CJK', 'ja', 'ltr', '<p>静かな朝、読者は本を開きました。文字の間隔と行の高さを確かめます。小さな画面でも文章が読みやすいことが大切です。</p><p lang="zh" xml:lang="zh">清晨，读者打开一本书。文字、标点和段落应当清晰可读。</p><p lang="ko" xml:lang="ko">조용한 아침에 독자는 책을 펼쳤습니다. 문장과 줄 간격을 확인합니다.</p>' * 20, ''),
    ]
    rtl = [
        ('فارسی', 'fa', 'rtl', ''.join(f'<p id="p{i}">{FA * 6}</p>' for i in range(40)), ''),
        ('العربية', 'ar', 'rtl', ''.join(f'<p>{AR * 6}</p>' for _ in range(40)), ''),
        ('فارسی و English', 'fa', 'rtl', '<p>او گفت: <bdi lang="en" xml:lang="en">“The reading position is safe.”</bdi> سپس خواندن را ادامه داد. شمارهٔ صفحه ۱۲۳ و تاریخ 2026 را بررسی کنید.</p>' * 50, ''),
        ('یادداشت و جدول', 'fa', 'rtl', '<p id="origin">این جمله یک یادداشت دارد<a epub:type="noteref" href="#note">۱</a>.</p><aside epub:type="footnote" id="note"><p>یادداشت کوتاه. <a href="#origin">بازگشت</a></p></aside>' + table, ''),
    ]
    write_epub('veil-typesetting-ltr', 'en', 'ltr', chapters)
    write_epub('veil-typesetting-rtl', 'fa', 'rtl', rtl)


if __name__ == '__main__':
    main()
