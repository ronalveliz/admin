package controladores;

import com.BiteBooking.backend.service.FileService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import servicios.FileService;

@CrossOrigin("*")
@RestController
@AllArgsConstructor
@Slf4j
public class FileController {

    private final servicios.FileService fileService;

    @GetMapping("files/{name:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String name){

        Resource file = fileService.load(name);
        return  ResponseEntity.ok(file);


    }




}
