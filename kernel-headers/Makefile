KERNEL_HEADERS_TARBALL := $(abspath $(CURDIR)/../prebuilt_kernel_headers.tar.gz)

.PHONY: all headers_install

headers_install:
	@mkdir -p $(O)
	@gzip -dc $(KERNEL_HEADERS_TARBALL) | tar -xf - -C $(O)

all:
	@true
