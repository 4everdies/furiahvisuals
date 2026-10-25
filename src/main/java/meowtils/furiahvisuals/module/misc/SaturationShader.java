package meowtils.furiahvisuals.module.misc;

import meowtils.furiahvisuals.render.post.PostFrame;
import meowtils.furiahvisuals.render.post.PostProgram;

final class SaturationShader {
   private static final String GRADE = "misc/saturation_grade.fsh";
   private static final String DOWNSCALE = "misc/saturation_downscale.fsh";
   private static PostProgram grade;
   private static PostProgram downscale;
   private static int uGradeSize;
   private static int uGradeValues;
   private static int uGradeDither;
   private static int uDownSize;
   private static int uDownTexel;

   private SaturationShader() {
   }

   private static void link() {
      if (grade == null) {
         grade = PostProgram.linkFile("saturation", "misc/saturation_grade.fsh", "uScene");
         uGradeSize = grade.uniform("uSize");
         uGradeValues = grade.uniform("uGrade");
         uGradeDither = grade.uniform("uDither");
         downscale = PostProgram.linkFile("saturation preview", "misc/saturation_downscale.fsh", "uScene");
         uDownSize = downscale.uniform("uSize");
         uDownTexel = downscale.uniform("uTexel");
      }
   }

   static void grade(int source, int w, int h, float saturation, float contrast, float vibrance, float amount, boolean dither) {
      link();
      grade.use();
      PostProgram.set(uGradeSize, w, h);
      PostProgram.set(uGradeValues, saturation, contrast, vibrance, amount);
      PostProgram.set(uGradeDither, dither ? 1.0F : 0.0F);
      PostFrame.bind(0, source);
      PostFrame.quad();
   }

   static void downscale(int source, int srcW, int srcH, int w, int h) {
      link();
      downscale.use();
      PostProgram.set(uDownSize, w, h);
      PostProgram.set(uDownTexel, 0.25F / w, 0.25F / h);
      PostFrame.bind(0, source);
      PostFrame.quad();
   }
}
