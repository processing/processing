// Copyright 2005 Caleb Epstein
// Copyright 2006 John Maddock
// Copyright 2010 Rene Rivera
// Distributed under the Boost Software License, Version 1.0. (See accompany-
// ing file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)

/*
 * Copyright (c) 1997
 * Silicon Graphics Computer Systems, Inc.
 *
 * Permission to use, copy, modify, distribute and sell this software
 * and its documentation for any purpose is hereby granted without fee,
 * provided that the above copyright notice appear in all copies and
 * that both that copyright notice and this permission notice appear
 * in supporting documentation.  Silicon Graphics makes no
 * representations about the suitability of this software for any
 * purpose.  It is provided "as is" without express or implied warranty.
 */

/*
 * Copyright notice reproduced from <boost/detail/limits.hpp>, from
 * which this code was originally taken.
 *
 * Modified by Caleb Epstein to use <endian.h> with GNU libc and to
 * defined the BOOST_ENDIAN macro.
 */

/*
 * Modifications for oscpp by Stefan Kersten
 *  - Change prefix from BOOST to OSCPP
 *  - Remove PDP endianness
 *  - Add OSCPP_BYTE_ORDER_* macros
 */

#ifndef OSCPP_ENDIAN_HPP_INCLUDED
#define OSCPP_ENDIAN_HPP_INCLUDED

#define OSCPP_BYTE_ORDER_BIG_ENDIAN    4321
#define OSCPP_BYTE_ORDER_LITTLE_ENDIAN 1234

// GNU libc offers the helpful header <endian.h> which defines
// __BYTE_ORDER

#if defined (__GLIBC__) || defined(__ANDROID__)
# include <endian.h>
# if (__BYTE_ORDER == __LITTLE_ENDIAN)
#  define OSCPP_LITTLE_ENDIAN
# elif (__BYTE_ORDER == __BIG_ENDIAN)
#  define OSCPP_BIG_ENDIAN
# else
#  error Unknown machine endianness detected.
# endif
# define OSCPP_BYTE_ORDER __BYTE_ORDER
#elif defined(_BIG_ENDIAN) && !defined(_LITTLE_ENDIAN) || \
    defined(__BIG_ENDIAN__) && !defined(__LITTLE_ENDIAN__) || \
    defined(_STLP_BIG_ENDIAN) && !defined(_STLP_LITTLE_ENDIAN)
# define OSCPP_BIG_ENDIAN
# define OSCPP_BYTE_ORDER OSCPP_BYTE_ORDER_BIG_ENDIAN
#elif defined(_LITTLE_ENDIAN) && !defined(_BIG_ENDIAN) || \
    defined(__LITTLE_ENDIAN__) && !defined(__BIG_ENDIAN__) || \
    defined(_STLP_LITTLE_ENDIAN) && !defined(_STLP_BIG_ENDIAN)
# define OSCPP_LITTLE_ENDIAN
# define OSCPP_BYTE_ORDER OSCPP_BYTE_ORDER_LITTLE_ENDIAN
#elif defined(__sparc) || defined(__sparc__) \
   || defined(_POWER) || defined(__powerpc__) \
   || defined(__ppc__) || defined(__hpux) || defined(__hppa) \
   || defined(_MIPSEB) || defined(_POWER) \
   || defined(__s390__)
# define OSCPP_BIG_ENDIAN
# define OSCPP_BYTE_ORDER OSCPP_BYTE_ORDER_BIG_ENDIAN
#elif defined(__i386__) || defined(__alpha__) \
   || defined(__ia64) || defined(__ia64__) \
   || defined(_M_IX86) || defined(_M_IA64) \
   || defined(_M_ALPHA) || defined(__amd64) \
   || defined(__amd64__) || defined(_M_AMD64) \
   || defined(__x86_64) || defined(__x86_64__) \
   || defined(_M_X64) || defined(__bfin__)

# define OSCPP_LITTLE_ENDIAN
# define OSCPP_BYTE_ORDER OSCPP_BYTE_ORDER_LITTLE_ENDIAN
#else
# error The file oscpp/endian.hpp needs to be set up for your CPU type.
#endif

#endif // OSCPP_ENDIAN_HPP_INCLUDED
