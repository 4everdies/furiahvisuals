package meowtils.furiahvisuals.image;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ImageLoader {
   public static final long MAX_BYTES = 26214400L;
   public static final int HARD_MAX_SIDE = 16384;
   private static final long MAX_DECODE_PIXELS = 16777216L;
   public static final String CANT_OPEN = "Can't find or open that file.";
   public static final String PICKER_PATTERN = "*.png;*.jpg;*.jpeg;*.jfif;*.gif;*.bmp;*.webp;*.avif;*.heic";
   private static final Logger LOG = LogManager.getLogger("Furiah's Visuals");
   private static final Set<String> REPORTED = new HashSet<>();

   private ImageLoader() {
   }

   public static boolean isImageName(String name) {
      String ext = extension(name);
      if (ext == null) {
         return false;
      } else if (".tga".equals(ext)) {
         return true;
      } else {
         for (ImageLoader.Format f : ImageLoader.Format.values()) {
            if (f != ImageLoader.Format.HTML && f != ImageLoader.Format.VIDEO && f != ImageLoader.Format.TEXT && f.matches(ext)) {
               return true;
            }
         }

         return false;
      }
   }

   static String extension(String name) {
      int dot = name.lastIndexOf(46);
      return dot > 0 && dot != name.length() - 1 ? name.substring(dot).toLowerCase(Locale.ROOT).trim() : null;
   }

   public static ImageLoader.Image load(File file, int maxSide, boolean shrink) throws ImageLoader.LoadException {
      byte[] bytes = read(file);

      try {
         ImageLoader.Image image = decode(bytes, file.getName(), maxSide, shrink);
         LOG.debug(
            "Image {}: {} {}x{}, loaded as {}x{}",
            new Object[]{file.getAbsolutePath(), image.format.shortName, image.fileWidth, image.fileHeight, image.width, image.height}
         );
         return image;
      } catch (ImageLoader.LoadException var11) {
         String key = file.getAbsolutePath() + "|" + bytes.length + "|" + file.lastModified();
         Set<String> set = REPORTED;
         boolean first;
         synchronized (set) {
            first = REPORTED.add(key);
         }

         if (first) {
            LOG.warn("Image {} not loaded: {} ({})", new Object[]{file.getAbsolutePath(), var11.getMessage(), diagnose(bytes), var11.getCause()});
         } else {
            LOG.debug("Image {} not loaded again: {}", new Object[]{file.getAbsolutePath(), var11.getMessage()});
         }

         throw var11;
      }
   }

   public static ImageLoader.Image decodeBytes(byte[] bytes, String name, int maxSide, boolean shrink) throws ImageLoader.LoadException {
      if (bytes.length > 26214400L) {
         throw new ImageLoader.LoadException(tooBig(bytes.length));
      } else {
         return decode(bytes, name, maxSide, shrink);
      }
   }

   public static byte[] readBytes(File file) throws ImageLoader.LoadException {
      return read(file);
   }

   public static int[] dimensions(byte[] bytes, String name) throws ImageLoader.LoadException {
      ImageLoader.Format format = detect(bytes);
      if (!format.readable) {
         throw new ImageLoader.LoadException(unsupported(format, extension(name)));
      } else {
         ImageReader reader = null;
         MemoryCacheImageInputStream in = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes));

         int[] var9;
         try {
            reader = readerFor(format);
            reader.setInput(in, true, true);
            int w = reader.getWidth(0);
            int h = reader.getHeight(0);
            if (format != ImageLoader.Format.JPEG || jpegOrientation(bytes) < 5) {
               return new int[]{w, h};
            }

            int[] nArray2 = new int[]{h, w};
            var9 = nArray2;
         } catch (RuntimeException | IOException var19) {
            throw new ImageLoader.LoadException(damaged(format, extension(name)), var19);
         } finally {
            if (reader != null) {
               reader.dispose();
            }

            try {
               in.close();
            } catch (IOException var18) {
            }
         }

         return var9;
      }
   }

   static byte[] read(File file) throws ImageLoader.LoadException {
      String path = file.getAbsolutePath();
      if (!file.isFile()) {
         LOG.warn("Image {} not loaded: it does not exist or is not a file (exists: {}, folder: {})", new Object[]{path, file.exists(), file.isDirectory()});
         throw new ImageLoader.LoadException("Can't find or open that file.");
      } else {
         long length = file.length();
         if (length > 26214400L) {
            LOG.warn("Image {} not loaded: {} bytes", new Object[]{path, length});
            throw new ImageLoader.LoadException(tooBig(length));
         } else {
            byte[] bytes;
            try {
               bytes = Files.readAllBytes(file.toPath());
            } catch (RuntimeException | IOException var7) {
               boolean cloud = isCloudPlaceholder(file);
               LOG.warn(
                  "Image {} not loaded: could not read it ({} bytes on disk{})",
                  new Object[]{path, length, cloud ? ", a cloud file that is not on this PC" : "", var7}
               );
               throw new ImageLoader.LoadException(
                  cloud
                     ? "Can't find or open that file. It's in OneDrive (or another cloud folder) but not on this PC: open it once, or choose \"Always keep on this device\", then try again."
                     : "Can't find or open that file.",
                  var7
               );
            }

            if (bytes.length > 26214400L) {
               throw new ImageLoader.LoadException(tooBig(bytes.length));
            } else {
               return bytes;
            }
         }
      }
   }

   private static boolean isCloudPlaceholder(File file) {
      try {
         Object attributes = Files.getAttribute(file.toPath(), "dos:attributes");
         if (attributes instanceof Integer) {
            int a = (Integer)attributes;
            return (a & 4460544) != 0;
         }
      } catch (LinkageError | Exception var3) {
      }

      return false;
   }

   static ImageLoader.Image decode(byte[] bytes, String name, int maxSide, boolean shrink) throws ImageLoader.LoadException {
      ImageLoader.Format format = detect(bytes);
      String ext = extension(name);
      if (!format.readable) {
         throw new ImageLoader.LoadException(unsupported(format, ext));
      } else {
         String problem;
         if (format == ImageLoader.Format.JPEG && (problem = jpegProblem(bytes)) != null) {
            throw new ImageLoader.LoadException(problem);
         } else {
            try {
               return decodeAs(bytes, format, maxSide, shrink);
            } catch (ImageLoader.LoadException var11) {
               throw var11;
            } catch (RuntimeException | IOException var12) {
               Exception first = var12;
               byte[] cleaned = format == ImageLoader.Format.PNG ? stripPng(bytes) : (format == ImageLoader.Format.JPEG ? stripJpeg(bytes) : null);
               if (cleaned != null) {
                  try {
                     ImageLoader.Image image = decodeAs(cleaned, format, maxSide, shrink);
                     LOG.info("Image {}: read without its color profile and optional chunks (the first try said {})", new Object[]{name, first.toString()});
                     return image;
                  } catch (RuntimeException | IOException var10) {
                     var12.addSuppressed(var10);
                  }
               }

               throw new ImageLoader.LoadException(damaged(format, ext), var12);
            }
         }
      }
   }

   private static ImageLoader.Image decodeAs(byte[] bytes, ImageLoader.Format format, int maxSide, boolean shrink) throws IOException, ImageLoader.LoadException {
      ImageReader reader = readerFor(format);
      MemoryCacheImageInputStream in = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes));

      ImageLoader.Image var18;
      try {
         reader.setInput(in, true, true);
         int w = reader.getWidth(0);
         int h = reader.getHeight(0);
         if (w <= 0 || h <= 0) {
            throw new IOException("the header says " + w + "x" + h);
         }

         if (w > 16384 || h > 16384) {
            throw new ImageLoader.LoadException(tooLarge(w, h, 16384));
         }

         if (!shrink && (w > maxSide || h > maxSide)) {
            throw new ImageLoader.LoadException(tooLarge(w, h, maxSide));
         }

         int f = 1;
         if (shrink) {
            while (ceilDiv(w, f) > maxSide || ceilDiv(h, f) > maxSide || (long)ceilDiv(w, f) * ceilDiv(h, f) > 16777216L) {
               f++;
            }
         }

         ImageReadParam param = reader.getDefaultReadParam();
         if (f > 1) {
            param.setSourceSubsampling(f, f, 0, 0);
         }

         BufferedImage image = reader.read(0, param);
         int[] argb = toArgb(image, format == ImageLoader.Format.PNG ? pngTransparentKey(bytes) : null);
         int iw = image.getWidth();
         int ih = image.getHeight();
         int fw = w;
         int fh = h;
         int orientation;
         if (format == ImageLoader.Format.JPEG && (orientation = jpegOrientation(bytes)) > 1 && orientation <= 8) {
            argb = orient(argb, iw, ih, orientation);
            if (orientation >= 5) {
               int t = iw;
               iw = ih;
               ih = t;
               fw = h;
               fh = w;
            }
         }

         ImageLoader.Image image2 = new ImageLoader.Image(iw, ih, argb, format, fw, fh);
         var18 = image2;
      } finally {
         reader.dispose();

         try {
            in.close();
         } catch (IOException var25) {
         }
      }

      return var18;
   }

   private static int ceilDiv(int a, int b) {
      return (a + b - 1) / b;
   }

   private static ImageReader readerFor(ImageLoader.Format format) throws IOException {
      String name = format == ImageLoader.Format.JPEG ? "jpeg" : format.name().toLowerCase(Locale.ROOT);
      Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(name);
      ImageReader other = null;

      while (readers.hasNext()) {
         ImageReader r = readers.next();
         if (r.getClass().getName().startsWith("com.sun.imageio.")) {
            if (other != null) {
               other.dispose();
            }

            return r;
         }

         if (other == null) {
            other = r;
         } else {
            r.dispose();
         }
      }

      if (other != null) {
         return other;
      } else {
         throw new IOException("Java has no " + name + " reader");
      }
   }

   private static int[] toArgb(BufferedImage image, int[] key) {
      int w = image.getWidth();
      int h = image.getHeight();
      int[] out = new int[w * h];
      ColorModel cm = image.getColorModel();
      boolean grey = cm.getColorSpace().getType() == 6 && cm.getNumComponents() <= 2 && !(cm instanceof IndexColorModel);
      WritableRaster raster = image.getRaster();
      if (grey) {
         int bands = raster.getNumBands();
         int greyBits = cm.getComponentSize(0);
         int alphaBits = bands > 1 ? cm.getComponentSize(1) : 8;
         boolean alpha = bands > 1 && cm.hasAlpha();
         int[] sample = new int[bands];

         for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
               raster.getPixel(x, y, sample);
               int g = to8(sample[0], greyBits);
               int a = alpha ? to8(sample[1], alphaBits) : (key != null && key.length == 1 && sample[0] == key[0] ? 0 : 255);
               out[y * w + x] = a << 24 | g << 16 | g << 8 | g;
            }
         }

         return out;
      } else {
         image.getRGB(0, 0, w, h, out, 0, w);
         if (key != null && key.length == 3 && !cm.hasAlpha() && raster.getNumBands() >= 3) {
            int[] sample = new int[raster.getNumBands()];

            for (int y = 0; y < h; y++) {
               for (int x = 0; x < w; x++) {
                  raster.getPixel(x, y, sample);
                  if (sample[0] == key[0] && sample[1] == key[1] && sample[2] == key[2]) {
                     int n = y * w + x;
                     out[n] &= 16777215;
                  }
               }
            }
         }

         return out;
      }
   }

   private static int to8(int value, int bits) {
      if (bits == 8) {
         return value & 0xFF;
      } else {
         int max = (1 << bits) - 1;
         return Math.round(value * 255.0F / max);
      }
   }

   static int[] orient(int[] src, int w, int h, int orientation) {
      int nw = orientation >= 5 ? h : w;
      int[] dst = new int[src.length];

      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            int dx;
            int dy;
            switch (orientation) {
               case 2:
                  dx = w - 1 - x;
                  dy = y;
                  break;
               case 3:
                  dx = w - 1 - x;
                  dy = h - 1 - y;
                  break;
               case 4:
                  dx = x;
                  dy = h - 1 - y;
                  break;
               case 5:
                  dx = y;
                  dy = x;
                  break;
               case 6:
                  dx = h - 1 - y;
                  dy = x;
                  break;
               case 7:
                  dx = h - 1 - y;
                  dy = w - 1 - x;
                  break;
               case 8:
                  dx = y;
                  dy = w - 1 - x;
                  break;
               default:
                  dx = x;
                  dy = y;
            }

            dst[dy * nw + dx] = src[y * w + x];
         }
      }

      return dst;
   }

   public static ImageLoader.Format detect(byte[] b) {
      int n = b.length;
      if (n == 0) {
         return ImageLoader.Format.EMPTY;
      } else if (starts(b, 0, 137, 80, 78, 71, 13, 10, 26, 10)) {
         return ImageLoader.Format.PNG;
      } else if (starts(b, 0, 255, 216, 255)) {
         return ImageLoader.Format.JPEG;
      } else if (ascii(b, 0, "GIF87a") || ascii(b, 0, "GIF89a")) {
         return ImageLoader.Format.GIF;
      } else if (ascii(b, 0, "RIFF") && ascii(b, 8, "WEBP")) {
         return ImageLoader.Format.WEBP;
      } else if (ascii(b, 4, "ftyp") && n >= 12) {
         return isoMedia(b);
      } else {
         int header;
         if (!ascii(b, 0, "BM")
            || n < 26
            || (header = le32(b, 14)) != 12 && header != 16 && header != 40 && header != 52 && header != 56 && header != 64 && header != 108 && header != 124) {
            if (starts(b, 0, 73, 73, 42, 0) || starts(b, 0, 77, 77, 0, 42)) {
               return ImageLoader.Format.TIFF;
            } else if (ascii(b, 0, "8BPS")) {
               return ImageLoader.Format.PSD;
            } else if (!starts(b, 0, 0, 0, 1, 0) && !starts(b, 0, 0, 0, 2, 0) || n < 6 || b[4] == 0 && b[5] == 0) {
               if (starts(b, 0, 255, 10) || starts(b, 0, 0, 0, 0, 12, 74, 88, 76, 32, 13, 10, 135, 10)) {
                  return ImageLoader.Format.JXL;
               } else {
                  return ascii(b, 0, "qoif") ? ImageLoader.Format.QOI : textLike(b);
               }
            } else {
               return ImageLoader.Format.ICO;
            }
         } else {
            return ImageLoader.Format.BMP;
         }
      }
   }

   private static ImageLoader.Format isoMedia(byte[] b) {
      String major = latin1(b, 8, 4);
      if (!major.equals("avif") && !major.equals("avis")) {
         int box = be32(b, 0);
         int end = Math.min(b.length, box > 16 && box < 4096 ? box : 64);
         boolean heif = major.startsWith("hei") || major.startsWith("hev") || major.equals("mif1") || major.equals("msf1");

         for (int p = 16; p + 4 <= end; p += 4) {
            String brand = latin1(b, p, 4);
            if (brand.equals("avif") || brand.equals("avis")) {
               return ImageLoader.Format.AVIF;
            }

            if (brand.startsWith("hei") || brand.startsWith("hev") || brand.equals("mif1")) {
               heif = true;
            }
         }

         return heif ? ImageLoader.Format.HEIC : ImageLoader.Format.VIDEO;
      } else {
         return ImageLoader.Format.AVIF;
      }
   }

   private static ImageLoader.Format textLike(byte[] b) {
      int start = starts(b, 0, 239, 187, 191) ? 3 : 0;
      int len;
      int cut = len = Math.min(b.length - start, 1024);

      while (cut > 0 && cut > len - 4 && (b[start + cut - 1] & 192) == 128) {
         cut--;
      }

      if (cut > 0 && (b[start + cut - 1] & 192) == 192) {
         cut--;
      }

      if (start + len < b.length) {
         len = cut;
      }

      String text;
      try {
         text = StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(b, start, len))
            .toString();
      } catch (CharacterCodingException var7) {
         return ImageLoader.Format.UNKNOWN;
      }

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c < ' ' && c != '\t' && c != '\n' && c != '\r' && c != '\f') {
            return ImageLoader.Format.UNKNOWN;
         }
      }

      String head = text.trim().toLowerCase(Locale.ROOT);
      if (!head.startsWith("<!doctype html") && !head.startsWith("<html") && !head.contains("<html")) {
         return !head.startsWith("<svg") && (!head.startsWith("<?xml") || !head.contains("<svg")) ? ImageLoader.Format.TEXT : ImageLoader.Format.SVG;
      } else {
         return ImageLoader.Format.HTML;
      }
   }

   static String jpegProblem(byte[] b) {
      int p = 2;

      while (p + 4 <= b.length) {
         if ((b[p] & 255) != 255) {
            return null;
         }

         int m = b[p + 1] & 255;
         if (m == 255) {
            p++;
         } else if (m != 1 && (m < 208 || m > 216)) {
            if (m == 217 || m == 218) {
               return null;
            }

            int len = be16(b, p + 2);
            if (len < 2) {
               return null;
            }

            boolean sof = m >= 192 && m <= 207 && m != 196 && m != 200 && m != 204;
            if (sof && p + 10 <= b.length) {
               int precision = b[p + 4] & 255;
               int components = b[p + 9] & 255;
               if (components == 4) {
                  return "This JPEG uses CMYK colors (made for printing), which the mod can't read. Save it as an RGB JPEG or as PNG and try again.";
               }

               boolean rare = m == 195 || m == 199 || m >= 201 || precision != 8;
               return rare ? "This JPEG uses a rare kind of compression the mod can't read. Save it again as a normal JPEG or as PNG and try again." : null;
            }

            p += 2 + len;
         } else {
            p += 2;
         }
      }

      return null;
   }

   static int jpegOrientation(byte[] b) {
      int p = 2;

      while (p + 4 <= b.length) {
         if ((b[p] & 255) != 255) {
            return 0;
         }

         int m = b[p + 1] & 255;
         if (m != 255) {
            if (m == 217 || m == 218) {
               return 0;
            }

            if (m != 1 && (m < 208 || m > 216)) {
               int len = be16(b, p + 2);
               if (len < 2) {
                  return 0;
               }

               if (m == 225 && len >= 16 && ascii(b, p + 4, "Exif") && b[p + 8] == 0 && b[p + 9] == 0) {
                  return exifOrientation(b, p + 10, Math.min(b.length, p + 2 + len));
               }

               p += 2 + len;
            } else {
               p += 2;
            }
         } else {
            p++;
         }
      }

      return 0;
   }

   private static int exifOrientation(byte[] b, int tiff, int end) {
      if (tiff + 8 > end) {
         return 0;
      } else {
         boolean little = b[tiff] == 73 && b[tiff + 1] == 73;
         if (little || b[tiff] == 77 && b[tiff + 1] == 77) {
            long ifd = tiff + (little ? le32(b, tiff + 4) & 4294967295L : be32(b, tiff + 4) & 4294967295L);
            if (ifd + 2L > end) {
               return 0;
            } else {
               int count = little ? le16(b, (int)ifd) : be16(b, (int)ifd);

               for (int i = 0; i < count; i++) {
                  int e = (int)ifd + 2 + i * 12;
                  if (e + 12 > end) {
                     return 0;
                  }

                  int tag = little ? le16(b, e) : be16(b, e);
                  if (tag == 274) {
                     return little ? le16(b, e + 8) : be16(b, e + 8);
                  }
               }

               return 0;
            }
         } else {
            return 0;
         }
      }
   }

   static int[] pngTransparentKey(byte[] b) {
      if (b.length < 33) {
         return null;
      } else {
         int colorType = b[25] & 255;
         if (colorType != 0 && colorType != 2) {
            return null;
         } else {
            int p = 8;

            while (p + 12 <= b.length) {
               int len = be32(b, p);
               if (len >= 0 && p + 12L + len <= b.length) {
                  if (!ascii(b, p + 4, "IDAT") && !ascii(b, p + 4, "IEND")) {
                     if (ascii(b, p + 4, "tRNS")) {
                        if (colorType == 0 && len >= 2) {
                           return new int[]{be16(b, p + 8)};
                        }

                        if (colorType == 2 && len >= 6) {
                           return new int[]{be16(b, p + 8), be16(b, p + 10), be16(b, p + 12)};
                        }

                        return null;
                     }

                     p += 12 + len;
                     continue;
                  }

                  return null;
               }

               return null;
            }

            return null;
         }
      }
   }

   static byte[] stripPng(byte[] b) {
      ByteArrayOutputStream out = new ByteArrayOutputStream(b.length);
      out.write(b, 0, 8);
      int p = 8;
      boolean ended = false;
      boolean data = false;

      int len;
      while (p + 12 <= b.length && (len = be32(b, p)) >= 0 && p + 12L + len <= b.length) {
         String type = latin1(b, p + 4, 4);
         if (type.equals("IHDR") || type.equals("PLTE") || type.equals("tRNS") || type.equals("IDAT") || type.equals("IEND")) {
            out.write(b, p, 12 + len);
            data |= type.equals("IDAT");
         }

         p += 12 + len;
         if (type.equals("IEND")) {
            ended = true;
            break;
         }
      }

      if (!data) {
         return null;
      } else {
         if (!ended) {
            out.write(new byte[]{0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126}, 0, 12);
         }

         return out.toByteArray();
      }
   }

   static byte[] stripJpeg(byte[] b) {
      ByteArrayOutputStream out = new ByteArrayOutputStream(b.length);
      out.write(b, 0, 2);
      int p = 2;

      while (p + 4 <= b.length) {
         if ((b[p] & 255) != 255) {
            return null;
         }

         int m = b[p + 1] & 255;
         if (m == 218) {
            out.write(b, p, b.length - p);
            return out.toByteArray();
         }

         if (m == 255) {
            p++;
         } else if (m != 1 && (m < 208 || m > 215)) {
            int len = be16(b, p + 2);
            if (len < 2 || p + 2 + len > b.length) {
               return null;
            }

            boolean optional = m >= 225 && m <= 239 && m != 238;
            if (!optional) {
               out.write(b, p, 2 + len);
            }

            p += 2 + len;
         } else {
            out.write(b, p, 2);
            p += 2;
         }
      }

      return null;
   }

   static String unsupported(ImageLoader.Format f, String ext) {
      String named = ext == null ? " with no extension" : (f.matches(ext) ? "" : " with a " + ext + " name");
      switch (f) {
         case EMPTY:
            return "This file is empty (0 bytes). Save the image again and try again.";
         case HTML:
            return "This file is a web page" + named + ", not an image. Open the image by itself in your browser, save it, and try again.";
         case TEXT:
            return "This file is text" + named + ", not an image.";
         case VIDEO:
            return "This file is a video" + named + ", not an image.";
         case SVG:
            return "This file is an SVG drawing" + named + ". Export it as PNG and try again.";
         case UNKNOWN:
            if (".tga".equals(ext)) {
               return "TGA images can't be used. Save it as PNG and try again.";
            }

            return ext == null
               ? "This file isn't a PNG, JPG, GIF or BMP image (or it's damaged)."
               : "This file has a " + ext + " name, but it isn't a PNG, JPG, GIF or BMP image (or it's damaged).";
         default:
            return named.isEmpty()
               ? f.shortName + " images can't be used. Save it as PNG and try again."
               : "This file is " + f.description + named + ". Save it as PNG and try again.";
      }
   }

   private static String damaged(ImageLoader.Format f, String ext) {
      String named = ext != null && !f.matches(ext) ? " (named " + ext + ")" : "";
      return "This "
         + f.shortName
         + " image"
         + named
         + " is damaged or incomplete, so it can't be read. Open it in an image editor, save it again and try again.";
   }

   private static String tooBig(long bytes) {
      return String.format(Locale.ROOT, "This file is %.1f MB; images can be up to 25 MB. Save a smaller version and try again.", bytes / 1048576.0);
   }

   private static String tooLarge(int w, int h, int max) {
      return "This image is " + w + "×" + h + " pixels; the most is " + max + " on each side. Scale it down and try again.";
   }

   static String diagnose(byte[] b) {
      StringBuilder sb = new StringBuilder();
      sb.append(b.length).append(" bytes, starts");

      for (int i = 0; i < Math.min(16, b.length); i++) {
         sb.append(' ').append(String.format(Locale.ROOT, "%02X", b[i] & 255));
      }

      return sb.toString();
   }

   private static boolean starts(byte[] b, int at, int... values) {
      if (at + values.length > b.length) {
         return false;
      } else {
         for (int i = 0; i < values.length; i++) {
            if ((b[at + i] & 255) != (values[i] & 0xFF)) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean ascii(byte[] b, int at, String s) {
      if (at >= 0 && at + s.length() <= b.length) {
         for (int i = 0; i < s.length(); i++) {
            if ((b[at + i] & 255) != s.charAt(i)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static String latin1(byte[] b, int at, int n) {
      return at + n > b.length ? "" : new String(b, at, n, StandardCharsets.ISO_8859_1);
   }

   private static int be16(byte[] b, int at) {
      return (b[at] & 0xFF) << 8 | b[at + 1] & 0xFF;
   }

   private static int le16(byte[] b, int at) {
      return (b[at + 1] & 0xFF) << 8 | b[at] & 0xFF;
   }

   private static int be32(byte[] b, int at) {
      return (b[at] & 0xFF) << 24 | (b[at + 1] & 0xFF) << 16 | (b[at + 2] & 0xFF) << 8 | b[at + 3] & 0xFF;
   }

   private static int le32(byte[] b, int at) {
      return (b[at + 3] & 0xFF) << 24 | (b[at + 2] & 0xFF) << 16 | (b[at + 1] & 0xFF) << 8 | b[at] & 0xFF;
   }

   public static enum Format {
      PNG("a PNG image", "PNG", true, ".png", ".apng"),
      JPEG("a JPEG image", "JPEG", true, ".jpg", ".jpeg", ".jpe", ".jfif", ".jif", ".pjpeg", ".pjp"),
      GIF("a GIF image", "GIF", true, ".gif"),
      BMP("a BMP image", "BMP", true, ".bmp", ".dib"),
      WEBP("a WebP image", "WebP", false, ".webp"),
      AVIF("an AVIF image", "AVIF", false, ".avif"),
      HEIC("a HEIC image", "HEIC", false, ".heic", ".heif", ".hif"),
      JXL("a JPEG XL image", "JPEG XL", false, ".jxl"),
      TIFF("a TIFF image", "TIFF", false, ".tif", ".tiff"),
      PSD("a Photoshop file", "Photoshop", false, ".psd"),
      ICO("an icon file (ICO)", "ICO", false, ".ico", ".cur"),
      QOI("a QOI image", "QOI", false, ".qoi"),
      SVG("an SVG drawing", "SVG", false, ".svg"),
      HTML("a web page", "HTML", false, ".html", ".htm"),
      VIDEO("a video", "video", false, ".mp4", ".mov", ".m4v", ".3gp"),
      TEXT("a text file", "text", false, ".txt"),
      EMPTY("an empty file", "empty", false),
      UNKNOWN("not an image", "unknown", false);

      public final String description;
      public final String shortName;
      public final boolean readable;
      private final String[] extensions;

      private Format(String description, String shortName, boolean readable, String... extensions) {
         this.description = description;
         this.shortName = shortName;
         this.readable = readable;
         this.extensions = extensions;
      }

      public boolean matches(String ext) {
         for (String e : this.extensions) {
            if (e.equals(ext)) {
               return true;
            }
         }

         return false;
      }

      public String extension() {
         return this.extensions.length > 0 ? this.extensions[0] : "";
      }
   }

   public static final class Image {
      public final int width;
      public final int height;
      public final int[] argb;
      public final ImageLoader.Format format;
      public final int fileWidth;
      public final int fileHeight;

      Image(int width, int height, int[] argb, ImageLoader.Format format, int fileWidth, int fileHeight) {
         this.width = width;
         this.height = height;
         this.argb = argb;
         this.format = format;
         this.fileWidth = fileWidth;
         this.fileHeight = fileHeight;
      }

      public BufferedImage toBufferedImage() {
         DataBufferInt buffer = new DataBufferInt(this.argb, this.argb.length);
         WritableRaster raster = Raster.createPackedRaster(buffer, this.width, this.height, this.width, new int[]{16711680, 65280, 255, -16777216}, null);
         return new BufferedImage(ColorModel.getRGBdefault(), raster, false, null);
      }
   }

   public static final class LoadException extends Exception {
      public LoadException(String message) {
         super(message);
      }

      public LoadException(String message, Throwable cause) {
         super(message, cause);
      }
   }
}
