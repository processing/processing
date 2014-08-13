/*
    Copyright 2012-2013 Samplecount S.L.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

#ifndef METHCLA_TYPES_H_INCLUDED
#define METHCLA_TYPES_H_INCLUDED

enum Methcla_NodePlacement
{
    kMethcla_NodePlacementHeadOfGroup,
    kMethcla_NodePlacementTailOfGroup,
    kMethcla_NodePlacementBeforeNode,
    kMethcla_NodePlacementAfterNode
};

enum Methcla_BusMappingFlags
{
    kMethcla_BusMappingInternal = 0x00
  , kMethcla_BusMappingExternal = 0x01
  , kMethcla_BusMappingFeedback = 0x02
  , kMethcla_BusMappingReplace  = 0x04
};

enum Methcla_NodeDoneFlags
{
    kMethcla_NodeDoneDoNothing          = 0x00
  , kMethcla_NodeDoneFreeSelf           = 0x01
  , kMethcla_NodeDoneFreePreceeding     = 0x02
  , kMethcla_NodeDoneFreeFollowing      = 0x04
  , kMethcla_NodeDoneFreeAllSiblings    = 0x08
  , kMethcla_NodeDoneFreeParent         = 0x10
};

#endif /* METHCLA_TYPES_H_INCLUDED */
