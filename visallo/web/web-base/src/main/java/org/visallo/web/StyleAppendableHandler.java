package org.visallo.web;

import com.asual.lesscss.LessEngine;
import com.asual.lesscss.LessOptions;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.RequestResponseHandler;
import org.apache.commons.io.IOUtils;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StyleAppendableHandler implements RequestResponseHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(Router.class);

    private LessEngine lessCompiler;
    private final List<Resource> resources = new ArrayList();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        response.setContentType("text/css");
        try (ServletOutputStream out = response.getOutputStream()) {
            Iterator var5 = resources.iterator();

            while (var5.hasNext()) {
                Resource resource = (Resource) var5.next();
                try {
                    resource.handle(out);
                    out.write("\n".getBytes());
                } catch (Exception e) {
                    LOGGER.error("Unable to process style resource: " + resource.getPath(), e);
                }
            }
        }
    }


    public void appendLessResource(String pathInfo) {
        resources.add(new LessResource(pathInfo));
    }

    public void appendCssResource(String pathInfo) {
        resources.add(new CssResource(pathInfo));
    }

    private synchronized LessEngine lessCompiler() {
        if (lessCompiler == null) {
            LessOptions options = new LessOptions();
            options.setCompress(true);
            options.setCharset("UTF-8");
            lessCompiler = new LessEngine(options);
        }
        return lessCompiler;
    }

    private interface Resource {
        public void handle(OutputStream out) throws Exception;

        public String getPath();
    }

    private class LessResource implements Resource {
        private String path;

        public LessResource(String path) {
            this.path = path;
        }

        @Override
        public String getPath() {
            return path;
        }

        public void handle(OutputStream out) throws Exception {
            try (InputStream input = this.getClass().getResourceAsStream(path)) {
                try (StringWriter writer = new StringWriter()) {
                    IOUtils.copy(input, writer, StandardCharsets.UTF_8);
                    String inputLess = writer.toString();
                    String output = lessCompiler().compile(inputLess);

                    PrintWriter outWriter = new PrintWriter(out, true);
                    outWriter.println(output);
                }
            }
        }
    }

    private class CssResource implements Resource {
        private String path;

        public CssResource(String path) {
            this.path = path;
        }

        @Override
        public String getPath() {
            return path;
        }

        public void handle(OutputStream out) throws IOException {
            try (InputStream in = this.getClass().getResourceAsStream(path)) {
                IOUtils.copy(in, out);
            }
        }
    }
}
