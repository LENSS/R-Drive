#include "edu_tamu_lenss_mdfs_crypto_ReedSolomon.h"
#include <stdio.h> // for printf
#include <stdlib.h> // for malloc
#include <jni.h>
#include "jerasure.h"
#include "reed_sol.h"
#include "galois.h"

#define WORDSIZE 8

/*
 ****************** JNI Decode function *********************
 */
JNIEXPORT jbyteArray JNICALL Java_edu_tamu_lenss_mdfs_crypto_ReedSolomon_decode
	  (JNIEnv * env, jobject jobj, jobjectArray jdataArray, jobjectArray jcodingArray,
			  jintArray jerasures, jintArray jerased, jlong filesize, jint blocksize,
			  jint k, jint n, jint w) {

	jbyteArray result;

	jbyte* resultPtr;
	int i, j;				// loop control
	int m = n-k;			// n-k is the size of the coding blocks

	jboolean isCopy;		// JNI variable set by VM

	jbyteArray byteArray;	// access the bytes in coding and data
	jbyte *elements;		// a pointer to the elements of byteArray

	// JErasure arguments
	char **data;
	char **coding;
	int *erasures;
	int *erased;
	int *matrix;

	matrix = NULL;

	// Allocate memory
	data=(char **)malloc(sizeof(char *)*k);
	for (i=0;i<k;i++) {
		data[i] = (char *)malloc(sizeof(char) * blocksize);
	}
	coding=(char**)malloc(sizeof(char *)*m);
	for (i=0;i<m;i++) {
		coding[i] = (char *)malloc(sizeof(char) * blocksize);
	}

	erasures = (*env)->GetIntArrayElements(env, jerasures, &isCopy);
	erased = (*env)->GetIntArrayElements(env, jerased, &isCopy);

	for (i=0;i<k;i++) {
		byteArray = (*env)->GetObjectArrayElement(env, jdataArray, i);
		// GetObjectArrayElements returns NULL if the fragments is erased
		if (byteArray != 0) {
			elements = (jbyte *)(*env)->GetByteArrayElements(env, byteArray, &isCopy);
			data[i] = (char *) elements;
			//(*env)->ReleaseByteArrayElements(env, byteArray, elements, 0);
		}
		(*env)->DeleteLocalRef(env, byteArray);
	}

	for (i=0;i<m;i++) {
		byteArray = (*env)->GetObjectArrayElement(env, jcodingArray, i);
		// GetObjectArrayElements returns NULL if the fragments is erased
		if (byteArray != 0) {
			elements = (jbyte *)(*env)->GetByteArrayElements(env, byteArray, &isCopy);
			coding[i] = (char *) elements;
			//(*env)->ReleaseByteArrayElements(env, byteArray, elements, 0);
		}
		(*env)->DeleteLocalRef(env, byteArray);
	}

	// generate the matrix
	matrix = reed_sol_vandermonde_coding_matrix(k, m, w);

	// Decoding method
	i = jerasure_matrix_decode(k, m, w, matrix, 1, erasures, data, coding, blocksize);

	// generate the return value as a jbyteArray
	result = (*env)->NewByteArray(env, filesize);
	resultPtr = (*env)->GetByteArrayElements(env, result, &isCopy);

	int total = 0; // total bytes written
	for (i=0;i<k;i++) {
		int base = i*blocksize;
		if (total+blocksize < filesize) {
			for (j=0; j<blocksize; j++) {
				resultPtr[base+j] = data[i][j];
			}
			total += blocksize;
		} else {
			for (j=0; j<blocksize; j++) {
				if (total < filesize) {
					resultPtr[base+j] = data[i][j];
					total++;
				} else {
					break;
				}
			}
		}
	}

	(*env)->ReleaseByteArrayElements(env, result, resultPtr, 0);

	//free(data);
	//free(coding);
	//free(erasures);
	//free(erased);

	return result;
}

/*
 ****************** JNI Encode function *********************
 */

JNIEXPORT jobjectArray JNICALL Java_edu_tamu_lenss_mdfs_crypto_ReedSolomon_encode
  (JNIEnv * env, jobject jobj, jbyteArray fileToEncode, jint k, jint n, jint w) {

	jobjectArray result;	// return value of type byte[][]
	int size, newsize, blocksize;
	int i;					// loop control
	int m = n-k;			// n-k is the size of the coding blocks

	jboolean iscopy;

	jbyte *block;
	jbyte *charArray;

	// JErasure arguments
	char **data;
	char **coding;
	int *matrix;


	//printf("Encoder called with k = %d and n = %d\n", k, n);
	//printf("The size of a jbyte is: %zu\n", sizeof(jbyte));
	//printf("The size of a char is: %zu\n", sizeof(char));

	size = (*env)->GetArrayLength(env, fileToEncode);
	//printf("The size of the byte Array is %d\n", size);

	newsize = size;
	// Find size by determining the next closest multiple
	if (size%(k*WORDSIZE*sizeof(char)) != 0) {
		while(newsize%(k*WORDSIZE*sizeof(char)) != 0)
			newsize++;
	}


	// determine the size of k and m files
	blocksize = newsize/k;
	//printf("The size of each block will be %d\n", blocksize);

	// copy fileToEncode to a char[]

	charArray = (*env)->GetByteArrayElements(env, fileToEncode, &iscopy);

	block= (jbyte *) malloc(sizeof(char) * newsize);
		for (i=0; i < size; i++) {
			block[i] = charArray[i];
	}

	// Allocate data and coding
	data = (char **)malloc(sizeof(char*)*k);
	coding = (char**)malloc(sizeof(char*)*m);
	for (i=0; i < m; i++) {
		coding[i] = (char *)malloc(sizeof(char)*blocksize);
	}

	// create the coding matrix
	matrix = reed_sol_vandermonde_coding_matrix(k, m, w);

	for (i=0; i<k; i++) {
		data[i] = (char *) (block + (i * blocksize));
	}

	jerasure_matrix_encode(k, m, w, matrix, data, coding, blocksize);

	// get the class for primitive jbyte
	jclass byteArrayClass = (*env)->FindClass(env, "[B");
	if (byteArrayClass == NULL) {
		return NULL; // an exception will be thrown
	}

	// create a new Object array of size n and type byteArray
	result = (*env)->NewObjectArray(env, n, byteArrayClass, NULL);


	// Add the data blocks
	for (i=0; i<k; i++) {
		jbyteArray jb = (*env)->NewByteArray(env, blocksize);
		(*env)->SetByteArrayRegion(env, jb, 0, blocksize, (jbyte*)data[i]);
		(*env)->SetObjectArrayElement(env, result, i, jb);
		(*env)->DeleteLocalRef(env, jb);
	}

	// Add the coding blocks to result
	for (i=0; i<m; i++) {
		jbyteArray jb = (*env)->NewByteArray(env, blocksize);
		(*env)->SetByteArrayRegion(env, jb, 0, blocksize, (jbyte*)coding[i]);
		(*env)->SetObjectArrayElement(env, result, i+k, jb);
		(*env)->DeleteLocalRef(env, jb);
	}

	(*env)->ReleaseByteArrayElements(env, fileToEncode, charArray, 0);

	free(block);

	return result;
}

JNIEXPORT void JNICALL Java_edu_tamu_lenss_mdfs_crypto_ReedSolomon_dummyCall
  (JNIEnv * env, jobject jobj) {
	printf("Dummy Call");
	return;
}
