# Lineage's generated_kernel_includes copies only Makefile and include/** of
# TARGET_KERNEL_SOURCE into its sandbox, so the header export lives in include/.
KERNEL_HEADERS_TARBALL := $(abspath $(CURDIR)/include/prebuilt_kernel_headers.tar.gz)

.PHONY: all headers_install

headers_install:
	@mkdir -p $(O)
	@gzip -dc $(KERNEL_HEADERS_TARBALL) | tar -xf - -C $(O)

all:
	@true
