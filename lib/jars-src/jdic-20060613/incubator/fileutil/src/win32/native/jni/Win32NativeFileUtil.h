/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_jdesktop_jdic_fileutil_Win32NativeFileUtil */

#ifndef _Included_org_jdesktop_jdic_fileutil_Win32NativeFileUtil
#define _Included_org_jdesktop_jdic_fileutil_Win32NativeFileUtil
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    getFreeSpace
 * Signature: (Ljava/lang/String;)[J
 */
JNIEXPORT jlongArray JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_getFreeSpace
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    getTotalSpace
 * Signature: (Ljava/lang/String;)[J
 */
JNIEXPORT jlongArray JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_getTotalSpace
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    recycle
 * Signature: (Ljava/lang/String;Z)I
 */
JNIEXPORT jint JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_recycle
  (JNIEnv *, jobject, jstring, jboolean);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    getFileSystem
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_getFileSystem
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    isArchive
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_isArchive
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    setArchive
 * Signature: (Ljava/lang/String;Z)Z
 */
JNIEXPORT jboolean JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_setArchive
  (JNIEnv *, jobject, jstring, jboolean);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    isNormal
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_isNormal
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    setNormal
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_setNormal
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    isReadOnly
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_isReadOnly
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    isSystem
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_isSystem
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    setSystem
 * Signature: (Ljava/lang/String;Z)Z
 */
JNIEXPORT jboolean JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_setSystem
  (JNIEnv *, jobject, jstring, jboolean);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    isTemporary
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_isTemporary
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    setTemporary
 * Signature: (Ljava/lang/String;Z)Z
 */
JNIEXPORT jboolean JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_setTemporary
  (JNIEnv *, jobject, jstring, jboolean);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    isCompressed
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_isCompressed
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    isEncrypted
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_isEncrypted
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    setHidden
 * Signature: (Ljava/lang/String;Z)Z
 */
JNIEXPORT jboolean JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_setHidden
  (JNIEnv *, jobject, jstring, jboolean);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    findFirst
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_findFirst
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    findNext
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_findNext
  (JNIEnv *, jobject);

/*
 * Class:     org_jdesktop_jdic_fileutil_Win32NativeFileUtil
 * Method:    findClose
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_jdesktop_jdic_fileutil_Win32NativeFileUtil_findClose
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
