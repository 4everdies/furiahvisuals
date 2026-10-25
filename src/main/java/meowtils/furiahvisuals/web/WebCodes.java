package meowtils.furiahvisuals.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class WebCodes {
   public static final String PREFIX = "FVP2-";
   private static final int MAX_JSON = 4194304;

   private WebCodes() {
   }

   public static String encode(JsonObject values) {
      byte[] json = JsJson.stringify(values).getBytes(StandardCharsets.UTF_8);
      Deflater deflater = new Deflater(-1);

      String var6;
      try {
         deflater.setInput(json);
         deflater.finish();
         ByteArrayOutputStream out = new ByteArrayOutputStream(json.length / 3 + 64);
         byte[] buf = new byte[8192];

         while (!deflater.finished()) {
            out.write(buf, 0, deflater.deflate(buf));
         }

         String string = "FVP2-" + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
         var6 = string;
      } finally {
         deflater.end();
      }

      return var6;
   }

   public static boolean isCode(String code) {
      return code != null && code.trim().startsWith("FVP2-");
   }

   public static JsonObject decode(String code) {
      if (!isCode(code)) {
         return null;
      } else {
         Inflater inflater = new Inflater();

         JsonObject var6;
         try {
            inflater.setInput(Base64.getUrlDecoder().decode(code.trim().substring("FVP2-".length())));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];

            do {
               if (inflater.finished()) {
                  JsonElement json = new JsonParser().parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
                  return json.isJsonObject() ? json.getAsJsonObject() : null;
               }

               int n = inflater.inflate(buf);
               if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                  return null;
               }

               out.write(buf, 0, n);
            } while (out.size() <= 4194304);

            JsonObject jsonObject = null;
            var6 = jsonObject;
         } catch (IllegalArgumentException | IllegalStateException | DataFormatException | JsonParseException var10) {
            return null;
         } finally {
            inflater.end();
         }

         return var6;
      }
   }
}
