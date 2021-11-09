#include <jni.h>
#include <string>

// extern声明在bspatch.c里面
extern "C" {
extern int main(int argc,const char * argv[]);
}

extern "C" JNIEXPORT void JNICALL
Java_com_inke_mybsdiff_MainActivity_bspatch(
        JNIEnv *env,
        jobject instance,
        jstring oldApk_,
        jstring patch_,
        jstring output_) {

    //把java字符串转为C/C++识别的字符串
    const char *oldApk = env->GetStringUTFChars(oldApk_, 0);
    const char *patch = env->GetStringUTFChars(patch_, 0);
    const char *output = env->GetStringUTFChars(output_, 0);

    //合成
    // bspatch,oldfile,newfile,patchfile
    const char * argv[] = {"", oldApk, output, patch};
    main(4, argv);
//
    //释放指针(Java字符串的变量)
    env->ReleaseStringUTFChars(oldApk_, oldApk);
    env->ReleaseStringUTFChars(patch_, patch);
    env->ReleaseStringUTFChars(output_, output);
}