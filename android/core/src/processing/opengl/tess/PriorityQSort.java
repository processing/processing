/*
** License Applicability. Except to the extent portions of this file are

** made subject to an alternative license as permitted in the SGI Free
** Software License B, Version 2.0 (the "License"), the contents of this
** file are subject only to the provisions of the License. You may not use
** this file except in compliance with the License. You may obtain a copy
** of the License at Silicon Graphics, Inc., attn: Legal Services, 1600
** Amphitheatre Parkway, Mountain View, CA 94043-1351, or at:
**
** http://oss.sgi.com/projects/FreeB
**
** Note that, as provided in the License, the Software is distributed on an
** "AS IS" basis, with ALL EXPRESS AND IMPLIED WARRANTIES AND CONDITIONS
** DISCLAIMED, INCLUDING, WITHOUT LIMITATION, ANY IMPLIED WARRANTIES AND
** CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A
** PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
**
** NOTE:  The Original Code (as defined below) has been licensed to Sun
** Microsystems, Inc. ("Sun") under the SGI Free Software License B
** (Version 1.1), shown above ("SGI License").   Pursuant to Section
** 3.2(3) of the SGI License, Sun is distributing the Covered Code to
** you under an alternative license ("Alternative License").  This
** Alternative License includes all of the provisions of the SGI License
** except that Section 2.2 and 11 are omitted.  Any differences between
** the Alternative License and the SGI License are offered solely by Sun
** and not by SGI.
**
** Original Code. The Original Code is: OpenGL Sample Implementation,
** Version 1.2.1, released January 26, 2000, developed by Silicon Graphics,
** Inc. The Original Code is Copyright (c) 1991-2000 Silicon Graphics, Inc.
** Copyright in any portions created by third parties is as indicated
** elsewhere herein. All Rights Reserved.
**
** Additional Notice Provisions: The application programming interfaces
** established by SGI in conjunction with the Original Code are The
** OpenGL(R) Graphics System: A Specification (Version 1.2.1), released
** April 1, 1999; The OpenGL(R) Graphics System Utility Library (Version
** 1.3), released November 4, 1998; and OpenGL(R) Graphics with the X
** Window System(R) (Version 1.3), released October 19, 1998. This software
** was created using the OpenGL(R) version 1.2.1 Sample Implementation
** published by SGI, but has not been independently verified as being
** compliant with the OpenGL(R) version 1.2.1 Specification.
**
** Author: Eric Veach, July 1994
** Java Port: Pepijn Van Eeckhoudt, July 2003
** Java Port: Nathan Parker Burg, August 2003
** Processing integration: Andres Colubri, February 2012
*/

package processing.opengl.tess;

class PriorityQSort extends PriorityQ {
    PriorityQHeap heap;
    Object[] keys;

    // JAVA: 'order' contains indices into the keys array.
    // This simulates the indirect pointers used in the original C code
    // (from Frank Suykens, Luciad.com).
    int[] order;
    int size, max;
    boolean initialized;
    PriorityQ.Leq leq;

    public PriorityQSort(PriorityQ.Leq leq) {
        heap = new PriorityQHeap(leq);

        keys = new Object[PriorityQ.INIT_SIZE];

        size = 0;
        max = PriorityQ.INIT_SIZE;
        initialized = false;
        this.leq = leq;
    }

/* really __gl_pqSortDeletePriorityQ */
    @Override
    void pqDeletePriorityQ() {
        if (heap != null) heap.pqDeletePriorityQ();
        order = null;
        keys = null;
    }

    private static boolean LT(PriorityQ.Leq leq, Object x, Object y) {
        return (!PriorityQ.LEQ(leq, y, x));
    }

    private static boolean GT(PriorityQ.Leq leq, Object x, Object y) {
        return (!PriorityQ.LEQ(leq, x, y));
    }

    private static void Swap(int[] array, int a, int b) {
        if (true) {
            int tmp = array[a];
            array[a] = array[b];
            array[b] = tmp;
        } else {

        }
    }

    private static class Stack {
        int p, r;
    }

/* really __gl_pqSortInit */
    @Override
    boolean pqInit() {
        int p, r, i, j;
        int piv;
        Stack[] stack = new Stack[50];
        for (int k = 0; k < stack.length; k++) {
            stack[k] = new Stack();
        }
        int top = 0;

        int seed = 2016473283;

        /* Create an array of indirect pointers to the keys, so that we
         * the handles we have returned are still valid.
         */
        order = new int[size + 1];
/* the previous line is a patch to compensate for the fact that IBM */
/* machines return a null on a malloc of zero bytes (unlike SGI),   */
/* so we have to put in this defense to guard against a memory      */
/* fault four lines down. from fossum@austin.ibm.com.               */
        p = 0;
        r = size - 1;
        for (piv = 0, i = p; i <= r; ++piv, ++i) {
            // indirect pointers: keep an index into the keys array, not a direct pointer to its contents
            order[i] = piv;
        }

        /* Sort the indirect pointers in descending order,
         * using randomized Quicksort
         */
        stack[top].p = p;
        stack[top].r = r;
        ++top;
        while (--top >= 0) {
            p = stack[top].p;
            r = stack[top].r;
            while (r > p + 10) {
                seed = Math.abs( seed * 1539415821 + 1 );
                i = p + seed % (r - p + 1);
                piv = order[i];
                order[i] = order[p];
                order[p] = piv;
                i = p - 1;
                j = r + 1;
                do {
                    do {
                        ++i;
                    } while (GT(leq, keys[order[i]], keys[piv]));
                    do {
                        --j;
                    } while (LT(leq, keys[order[j]], keys[piv]));
                    Swap(order, i, j);
                } while (i < j);
                Swap(order, i, j);    /* Undo last swap */
                if (i - p < r - j) {
                    stack[top].p = j + 1;
                    stack[top].r = r;
                    ++top;
                    r = i - 1;
                } else {
                    stack[top].p = p;
                    stack[top].r = i - 1;
                    ++top;
                    p = j + 1;
                }
            }
            /* Insertion sort small lists */
            for (i = p + 1; i <= r; ++i) {
                piv = order[i];
                for (j = i; j > p && LT(leq, keys[order[j - 1]], keys[piv]); --j) {
                    order[j] = order[j - 1];
                }
                order[j] = piv;
            }
        }
        max = size;
        initialized = true;
        heap.pqInit();    /* always succeeds */

/*        #ifndef NDEBUG
        p = order;
        r = p + size - 1;
        for (i = p; i < r; ++i) {
            Assertion.doAssert(LEQ(     * * (i + 1), **i ));
        }
        #endif*/

        return true;
    }

/* really __gl_pqSortInsert */
/* returns LONG_MAX iff out of memory */
    @Override
    int pqInsert(Object keyNew) {
        int curr;

        if (initialized) {
            return heap.pqInsert(keyNew);
        }
        curr = size;
        if (++size >= max) {
            Object[] saveKey = keys;

            /* If the heap overflows, double its size. */
            max <<= 1;
//            pq->keys = (PQHeapKey *)memRealloc( pq->keys,(size_t)(pq->max * sizeof( pq->keys[0] )));
            Object[] pqKeys = new Object[max];
            System.arraycopy( keys, 0, pqKeys, 0, keys.length );
            keys = pqKeys;
            if (keys == null) {
                keys = saveKey;    /* restore ptr to free upon return */
                return Integer.MAX_VALUE;
            }
        }
        assert curr != Integer.MAX_VALUE;
        keys[curr] = keyNew;

        /* Negative handles index the sorted array. */
        return -(curr + 1);
    }

/* really __gl_pqSortExtractMin */
    @Override
    Object pqExtractMin() {
        Object sortMin, heapMin;

        if (size == 0) {
            return heap.pqExtractMin();
        }
        sortMin = keys[order[size - 1]];
        if (!heap.pqIsEmpty()) {
            heapMin = heap.pqMinimum();
            if (LEQ(leq, heapMin, sortMin)) {
                return heap.pqExtractMin();
            }
        }
        do {
            --size;
        } while (size > 0 && keys[order[size - 1]] == null);
        return sortMin;
    }

/* really __gl_pqSortMinimum */
    @Override
    Object pqMinimum() {
        Object sortMin, heapMin;

        if (size == 0) {
            return heap.pqMinimum();
        }
        sortMin = keys[order[size - 1]];
        if (!heap.pqIsEmpty()) {
            heapMin = heap.pqMinimum();
            if (PriorityQ.LEQ(leq, heapMin, sortMin)) {
                return heapMin;
            }
        }
        return sortMin;
    }

/* really __gl_pqSortIsEmpty */
    @Override
    boolean pqIsEmpty() {
        return (size == 0) && heap.pqIsEmpty();
    }

/* really __gl_pqSortDelete */
    @Override
    void pqDelete(int curr) {
        if (curr >= 0) {
            heap.pqDelete(curr);
            return;
        }
        curr = -(curr + 1);
        assert curr < max && keys[curr] != null;

        keys[curr] = null;
        while (size > 0 && keys[order[size - 1]] == null) {
            --size;
        }
    }
}
