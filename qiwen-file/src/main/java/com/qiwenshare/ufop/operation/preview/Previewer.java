package com.qiwenshare.ufop.operation.preview;

import java.io.InputStream;

public interface Previewer {
    InputStream preview(PreviewFile previewFile);
}
