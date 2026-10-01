/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * HyperOS libprocessgroup exports unmangled SetTaskProfiles and
 * SetProcessProfiles wrappers that Android 16 dropped; the stock camera
 * blobs (libcameraopt) link against them. The C++ overloads keep the same
 * parameters, so the exported names are set with asm labels.
 */

#include <processgroup/processgroup.h>

#include <string>
#include <vector>

bool ShimSetTaskProfiles(int tid, const std::vector<std::string>& profiles,
                         bool use_fd_cache) __asm__("SetTaskProfiles");
bool ShimSetTaskProfiles(int tid, const std::vector<std::string>& profiles,
                         bool use_fd_cache) {
    return SetTaskProfiles(tid, profiles, use_fd_cache);
}

bool ShimSetProcessProfiles(uid_t uid, pid_t pid, const std::vector<std::string>& profiles)
        __asm__("SetProcessProfiles");
bool ShimSetProcessProfiles(uid_t uid, pid_t pid, const std::vector<std::string>& profiles) {
    return SetProcessProfiles(uid, pid, profiles);
}
