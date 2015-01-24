package edu.stanford.ee368.hdrserver;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

public class HdrbuilderServlet extends HttpServlet {
	HDRMerge merger;
	String[] imagePaths;
	static final long serialVersionUID = 0x23148239;
	final static int N_PICTURES = 4;
	private static final Logger log =
		Logger.getLogger(HdrbuilderServlet.class.getName());
	/**
	 * Handles the post request
	 */
	public void doPost(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		imagePaths = new String[N_PICTURES];
		try {
			ServletFileUpload upload = new ServletFileUpload();
			res.setContentType("image/jpeg");

			FileItemIterator iterator = upload.getItemIterator(req);
			int i = 0;
			while (iterator.hasNext()) {
				FileItemStream item = iterator.next();
				InputStream stream = item.openStream();
				if (item.isFormField()) {
					log.warning("Got a form field: " + item.getFieldName());
				} else {
					log.warning("Got an uploaded file: " + item.getFieldName() +
							", name = " + item.getName());
					imagePaths[i] = String.format("/tmp/tmpfile%d.jpg", i);
					FileOutputStream outStream = new FileOutputStream(imagePaths[i++]);
					IOUtils.copy(stream, outStream);
					outStream.flush();
					outStream.close();
					stream.close();
				}
				
				
			}
		} catch (Exception ex) {
			throw new ServletException(ex);
		}
		double exposures[] = {1, 2, 4, 8};
		merger = new HDRMerge(imagePaths, exposures);
		String path = "/tmp/result.jpg";
		merger.toFile(path);
		res.setContentType("image/jpeg");
	    FileInputStream in = new FileInputStream(path);
	    IOUtils.copy(in, res.getOutputStream());
	    in.close();
	}
}