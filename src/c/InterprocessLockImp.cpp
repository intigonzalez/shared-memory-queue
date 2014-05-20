#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#include <sys/types.h>
#include <fcntl.h>

#include <map>
#include <string>
#include <atomic>

#include "InterprocessLock.h"

struct SharedPtr {
	pthread_mutex_t mutex;
	pthread_cond_t cv;
	pthread_cond_t cv2;
	int started;
};

#define MAX_LOCK_NUMBER 100

//struct SharedPtr* lockObjecs;

std::map<std::string, struct SharedPtr*> map;
std::map<std::string, int> map2;
std::atomic_int atomic(0);


struct SharedPtr* values[MAX_LOCK_NUMBER];

void checkError(const char* msg, int v) {
	if (v) {
		fprintf(stderr, "Error while: %s \n", msg);
		exit(1);
	}
	else
		printf("Ok: %s\n", msg);
}

void* initMemory(const char* name) {
    int fd = shm_open(name, O_RDWR | O_CREAT, S_IRUSR | S_IWUSR);
    if (fd == -1) {
        fd = shm_open(name, O_RDWR, S_IRUSR | S_IWUSR);
        if (fd == -1)
            exit(2);
    }

    ftruncate(fd, sizeof(struct SharedPtr));

    void* v = (struct SharedPtr*)
        mmap(NULL, sizeof(struct SharedPtr),
                PROT_READ | PROT_WRITE,
                MAP_SHARED, fd, 0);

    if (v == (void*)-1) {
        fprintf(stderr, "Error creating the shared memory %s\n", name);
        exit(1);
    }

    return v;
}

int create_shared_mutex(char* id, int initialize)
{
//    printf("Accessing lock %s\n", id);

    std::string s = id;
    if (map2.find(s) != map2.end())
        return map2[s];

    struct SharedPtr * m = (struct SharedPtr *)initMemory(id);

    int index = atomic.fetch_add(1);
    values[index] = m;
    map2[s] = index;

	if (initialize) {
		pthread_mutexattr_t mattr;
		pthread_condattr_t cattr;
	
		/* Process Shared Mutex */
		int rc = pthread_mutexattr_init(&mattr);
		rc = pthread_mutexattr_setpshared(&mattr, PTHREAD_PROCESS_SHARED);
		rc = pthread_mutex_init(&m->mutex, &mattr);
		/* Process Shared Condition */
		rc = pthread_condattr_init(&cattr);
 		rc = pthread_condattr_setpshared(&cattr, PTHREAD_PROCESS_SHARED);
  		rc = pthread_cond_init(&m->cv, &cattr);

  		/* Process Shared Condition */
        rc = pthread_condattr_init(&cattr);
        rc = pthread_condattr_setpshared(&cattr, PTHREAD_PROCESS_SHARED);
        rc = pthread_cond_init(&m->cv2, &cattr);
	}
	return index;
}

struct SharedPtr * get_shared_mutex(int id) {
    return values[id];
}

/*
 * Class:     InterprocessLock
 * Method:    lock
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_kevoree_microsandbox_javase_components_InterprocessLock_lock
  (JNIEnv * env, jclass clazz, jint id)
{
    struct SharedPtr * m = get_shared_mutex(id);
    pthread_mutex_lock(&m->mutex);
}

/*
 * Class:     InterprocessLock
 * Method:    unlock
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_kevoree_microsandbox_javase_components_InterprocessLock_unlock
  (JNIEnv * env, jclass clazz, jint id)
{
    struct SharedPtr * m = get_shared_mutex(id);
    pthread_mutex_unlock(&m->mutex);
}

/*
 * Class:     InterprocessLock
 * Method:    createNew
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_kevoree_microsandbox_javase_components_InterprocessLock_createNew
  (JNIEnv * env, jclass clazz, jbyteArray id, jboolean b)
{
    int len = env->GetArrayLength (id);
    char* buf = new char[len + 2];
    buf++;
    env->GetByteArrayRegion (id, 0, len, reinterpret_cast<jbyte*>(buf));
    buf--;
    buf[len] = 0;
    buf[0] = '/';
    int idx = create_shared_mutex(buf, b);
//    delete[] buff;
    return idx;
}

/*
 * Class:     InterprocessLock
 * Method:    initTable
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_kevoree_microsandbox_javase_components_InterprocessLock_initTable
  (JNIEnv * env, jclass clazz)
{
//    initMemory("/memory0", 1);
}


/*
 * Class:     InterprocessLock
 * Method:    waitInterprocess
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_kevoree_microsandbox_javase_components_InterprocessLock_waitInterprocess
  (JNIEnv * env, jclass clazz, jint id)
{
    struct SharedPtr * m = get_shared_mutex(id);
    pthread_cond_wait(&m->cv, &m->mutex);
}

/*
 * Class:     InterprocessLock
 * Method:    broadcast
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_kevoree_microsandbox_javase_components_InterprocessLock_broadcast
  (JNIEnv * env, jclass clazz, jint id)
{
    struct SharedPtr * m = get_shared_mutex(id);
    pthread_cond_broadcast(&m->cv);
}

/*
 * Class:     InterprocessLock
 * Method:    waitInterprocess2
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_kevoree_microsandbox_javase_components_InterprocessLock_waitInterprocess2
  (JNIEnv * env, jclass clazz, jint id)
{
    struct SharedPtr * m = get_shared_mutex(id);
    pthread_cond_wait(&m->cv2, &m->mutex);
}

/*
 * Class:     InterprocessLock
 * Method:    broadcast2
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_kevoree_microsandbox_javase_components_InterprocessLock_broadcast2
  (JNIEnv * env, jclass clazz, jint id)
{
    struct SharedPtr * m = get_shared_mutex(id);
    pthread_cond_broadcast(&m->cv2);
}