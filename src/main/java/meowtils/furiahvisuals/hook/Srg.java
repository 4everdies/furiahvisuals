package meowtils.furiahvisuals.hook;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import meowtils.furiahvisuals.core.FuriahsVisuals;

/**
 * Traduz nomes MCP (usados no codigo) para nomes SRG (field_XXXX / func_XXXX), que sao os
 * nomes reais das classes do Minecraft quando o mod roda fora do ambiente de desenvolvimento.
 *
 * A tabela {@code /furiah_srg.txt} e gerada pelo build.gradle (tarefa generateSrgTable) a partir
 * do mcp-srg.srg do ForgeGradle. Linhas: {@code F|M <classe/interna> <nomeMcp> <nomeSrg>}.
 * Sem a tabela, so os nomes MCP funcionam (ou seja, so o runClient).
 */
public final class Srg {
   private static final Map<String, List<String>> FIELDS = new HashMap<>();
   private static final Map<String, List<String>> METHODS = new HashMap<>();
   private static boolean loaded;
   private static int entries;

   private Srg() {
   }

   private static synchronized void load() {
      if (loaded) {
         return;
      }
      loaded = true;
      try (InputStream in = Srg.class.getResourceAsStream("/furiah_srg.txt")) {
         if (in == null) {
            FuriahsVisuals.LOG.warn("Hooks: furiah_srg.txt is missing from the jar; private Minecraft members are only found by their MCP names (works in runClient, NOT in a normal launcher)");
            return;
         }
         BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
         String line;
         while ((line = r.readLine()) != null) {
            String[] p = line.trim().split(" ");
            if (p.length != 4) {
               continue;
            }
            Map<String, List<String>> target = p[0].equals("F") ? FIELDS : p[0].equals("M") ? METHODS : null;
            if (target == null) {
               continue;
            }
            List<String> list = target.get(p[1] + "/" + p[2]);
            if (list == null) {
               list = new ArrayList<>(1);
               target.put(p[1] + "/" + p[2], list);
            }
            if (!list.contains(p[3])) {
               list.add(p[3]);
            }
            entries++;
         }
         FuriahsVisuals.LOG.info("Hooks: loaded {} MCP->SRG names", entries);
      } catch (Throwable t) {
         FuriahsVisuals.LOG.warn("Hooks: could not read furiah_srg.txt", t);
      }
   }

   /** Os nomes pedidos primeiro (MCP), depois os equivalentes SRG. */
   public static String[] names(Class<?> owner, boolean method, String... names) {
      load();
      Set<String> out = new LinkedHashSet<>();
      for (String n : names) {
         out.add(n);
      }
      String base = owner.getName().replace('.', '/') + "/";
      Map<String, List<String>> table = method ? METHODS : FIELDS;
      for (String n : names) {
         List<String> srg = table.get(base + n);
         if (srg != null) {
            out.addAll(srg);
         }
      }
      return out.toArray(new String[0]);
   }
}
