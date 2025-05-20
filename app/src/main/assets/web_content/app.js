      document.addEventListener("DOMContentLoaded", () => {
        const btnFeed = document.getElementById("btn-feed");
        const btnSubir = document.getElementById("btn-subir");
        const btnCarrito = document.getElementById("btn-carrito");
        const btnSubirProducto = document.getElementById("btn-subir-producto");
        const btnTomarFoto = document.getElementById("btn-tomar-foto");
        const abrirCamara = document.getElementById("abrirCamara");
        const productosFeed = document.getElementById("productos-feed");

        const sectionFeed = document.getElementById("feed");
        const sectionProductos = document.getElementById("productos");
        const sectionCarrito = document.getElementById("carrito");
        const carritoItems = document.getElementById("carrito-items");
        const carritoCantidad = document.getElementById("carrito-cantidad");
        const carritoTotal = document.getElementById("carrito-total");
        const video = document.getElementById("video");
        const canvas = document.getElementById("canvas");
        const ctx = canvas.getContext("2d");
        const preview = document.getElementById("preview");

        let carrito = [];
        let stream = null;

        function mostrarSeccion(seccion) {
          sectionFeed.style.display = "none";
          sectionProductos.style.display = "none";
          sectionCarrito.style.display = "none";
          if (seccion) seccion.style.display = "block";
        }

        btnFeed.addEventListener("click", () => mostrarSeccion(sectionFeed));
        btnSubir.addEventListener("click", () => mostrarSeccion(sectionProductos));
        btnCarrito.addEventListener("click", () => mostrarSeccion(sectionCarrito));

        abrirCamara.addEventListener("click", () => {
          document.querySelector(".camera-container").style.display = "block";
          iniciarCamara();
        });

        async function iniciarCamara() {
          try {
            stream = await navigator.mediaDevices.getUserMedia({ video: true });
            video.srcObject = stream;
          } catch (error) {
            console.error("Error al acceder a la cámara", error);
          }
        }

        btnTomarFoto?.addEventListener("click", () => {
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
          const imagenTomada = canvas.toDataURL("image/png");
          preview.src = imagenTomada;
          preview.style.display = "block";
          preview.dataset.fromCamera = "true";
        });

        btnSubirProducto?.addEventListener("click", async () => {
          const nombre = document.getElementById("nombrePrenda").value;
          const precio = document.getElementById("precioPrenda").value;
          const descripcion = document.getElementById("descripcionPrenda").value;
          const imagenInput = document.getElementById("imagenPrenda");
          const imagenArchivo = imagenInput.files[0];

          let imagenSrc = preview.dataset.fromCamera === "true" ? preview.src : null;
          let imageUrl = null;

          if (!nombre || !precio || !descripcion || (!imagenSrc && !imagenArchivo)) {
            alert("Por favor, completa todos los campos y selecciona o toma una imagen.");
            return;
          }

          try {
            if (imagenSrc) {
              imageUrl = await subirImagenFirebase(imagenSrc, nombre);
            } else if (imagenArchivo) {
              const reader = new FileReader();
              reader.onload = async (e) => {
                imageUrl = await subirImagenFirebase(e.target.result, nombre);
                if (imageUrl) {
                  await guardarProductoFirestore(nombre, precio, descripcion, imageUrl);
                  limpiarFormulario();
                }
              };
              reader.readAsDataURL(imagenArchivo);
              return;
            }

            if (imageUrl) {
              await guardarProductoFirestore(nombre, precio, descripcion, imageUrl);
              limpiarFormulario();
            }
          } catch (error) {
            console.error("Error al subir producto", error);
            alert("Hubo un error al subir el producto");
          }
        });

        async function subirImagenFirebase(imagenDataUrl, nombreProducto) {
          try {
            const storageRef = storage.ref();
            const imageName = `productos/${Date.now()}_${nombreProducto.replace(/\s+/g, '_').toLowerCase()}`;
            const imageRef = storageRef.child(imageName);
            const uploadTask = imageRef.putString(imagenDataUrl, 'data_url');
            await uploadTask;
            const downloadURL = await uploadTask.snapshot.ref.getDownloadURL();
            return downloadURL;
          } catch (error) {
            console.error("Error al subir la imagen a Firebase:", error);
            return null;
          }
        }

        async function guardarProductoFirestore(nombre, precio, descripcion, imageUrl) {
          try {
            await db.collection("productos").add({
              nombre,
              precio: parseFloat(precio),
              descripcion,
              imageUrl,
              timestamp: firebase.firestore.FieldValue.serverTimestamp()
            });
            alert("Producto subido exitosamente.");
            cargarProductos();
            mostrarSeccion(sectionFeed);
          } catch (error) {
            console.error("Error al guardar el producto en Firestore:", error);
          }
        }

        function limpiarFormulario() {
          document.getElementById("nombrePrenda").value = "";
          document.getElementById("precioPrenda").value = "";
          document.getElementById("descripcionPrenda").value = "";
          document.getElementById("imagenPrenda").value = "";
          preview.src = "";
          preview.style.display = "none";
          preview.dataset.fromCamera = "false";
          document.querySelector(".camera-container").style.display = "none";
          if (stream) {
            stream.getTracks().forEach(track => track.stop());
            video.srcObject = null;
          }
        }

        async function eliminarProducto(id, imageUrl) {
          try {
            await db.collection("productos").doc(id).delete();
            const imageRef = storage.refFromURL(imageUrl);
            await imageRef.delete();
            alert("Producto eliminado.");
            cargarProductos();
          } catch (error) {
            console.error("Error eliminando producto:", error);
            alert("Error al eliminar el producto");
          }
        }

        async function cargarProductos() {
          productosFeed.innerHTML = "";
          try {
            const snapshot = await db.collection("productos").orderBy("timestamp", "desc").get();
            snapshot.forEach(doc => {
              const producto = doc.data();
              const productoHTML = document.createElement("div");
              productoHTML.classList.add("producto");
              productoHTML.innerHTML = `
                <img src="${producto.imageUrl}" alt="${producto.nombre}" class="producto-img" style="max-width: 250px; height: auto; cursor: pointer;">
                <h3>${producto.nombre}</h3>
                <p>${producto.descripcion}</p>
                <span class="producto-precio">$${producto.precio.toFixed(2)}</span>
                <button class="btn-agregar-carrito" data-id="${doc.id}" data-nombre="${producto.nombre}" data-precio="${producto.precio}" data-img="${producto.imageUrl}">Agregar al carrito</button>
                <button class="btn-eliminar-feed" data-id="${doc.id}" data-img="${producto.imageUrl}">Eliminar</button>
              `;
              productosFeed.appendChild(productoHTML);
            });

            document.querySelectorAll(".btn-agregar-carrito").forEach(btn => {
              btn.addEventListener("click", () => {
                const producto = {
                  id: btn.dataset.id,
                  nombre: btn.dataset.nombre,
                  precio: parseFloat(btn.dataset.precio),
                  imageUrl: btn.dataset.img
                };
                agregarAlCarrito(producto);
              });
            });

            document.querySelectorAll(".btn-eliminar-feed").forEach(btn => {
              btn.addEventListener("click", () => {
                const id = btn.dataset.id;
                const imageUrl = btn.dataset.img;
                if (confirm("¿Seguro que quieres eliminar este producto?")) {
                  eliminarProducto(id, imageUrl);
                }
              });
            });

            document.querySelectorAll(".producto-img").forEach(img => {
              img.addEventListener("click", () => {
                const modal = document.getElementById("modalImagen");
                const imagenAmpliada = document.getElementById("imagenAmpliada");
                imagenAmpliada.src = img.src;
                modal.style.display = "flex";
              });
            });

          } catch (error) {
            console.error("Error al cargar productos:", error);
          }
        }

        function agregarAlCarrito(producto) {
          carrito.push(producto);
          actualizarCarrito();
        }

        function actualizarCarrito() {
          carritoItems.innerHTML = "";
          let total = 0;

          carrito.forEach((producto, index) => {
            const item = document.createElement("div");
            item.classList.add("carrito-item");
            item.innerHTML = `
              <img src="${producto.imageUrl}" alt="${producto.nombre}" class="carrito-img" style="width: 80px; height: auto; border-radius: 8px; margin-right: 10px; cursor: pointer;">
              <div>
                <h4>${producto.nombre}</h4>
                <p>$${producto.precio.toFixed(2)}</p>
              </div>
              <button class="btn-eliminar" data-index="${index}">Eliminar</button>
            `;
            carritoItems.appendChild(item);
            total += producto.precio;
          });

          carritoCantidad.textContent = carrito.length;
          carritoTotal.textContent = `$${total.toFixed(2)}`;

          document.querySelectorAll(".btn-eliminar").forEach(btn => {
            btn.addEventListener("click", () => {
              const index = parseInt(btn.dataset.index);
              carrito.splice(index, 1);
              actualizarCarrito();
            });
          });

          // Ampliar imagen al hacer clic
          document.querySelectorAll(".carrito-img").forEach(img => {
            img.addEventListener("click", () => {
              const modal = document.getElementById("modalImagen");
              const imagenAmpliada = document.getElementById("imagenAmpliada");
              imagenAmpliada.src = img.src;
              modal.style.display = "flex";
            });
          });

          // Botón de pagar por WhatsApp
          const existingBtn = document.getElementById("btn-pagar-whatsapp");
          if (carrito.length > 0 && !existingBtn) {
            const btnPagar = document.createElement("button");
            btnPagar.textContent = "Pagar por WhatsApp";
            btnPagar.id = "btn-pagar-whatsapp";
            btnPagar.classList.add("btn-principal");
            btnPagar.style.marginTop = "20px";
            btnPagar.addEventListener("click", () => {
              const mensaje = generarMensajeWhatsApp();
              const numero = "573232006662"; // Cambia por tu número con código país
              const url = `https://wa.me/${numero}?text=${encodeURIComponent(mensaje)}`;
              window.open(url, "_blank");
            });
            carritoItems.appendChild(btnPagar);
          } else if (carrito.length === 0 && existingBtn) {
            // Si el carrito queda vacío, quitar el botón
            existingBtn.remove();
          }
        }

        function generarMensajeWhatsApp() {
          let mensaje = "Hola, quiero comprar estos productos en ECOTREND:\n\n";
          let total = 0;

          carrito.forEach((producto, i) => {
            mensaje += `${i + 1}. ${producto.nombre} - $${producto.precio.toFixed(2)}\n`;
            total += producto.precio;
          });

          mensaje += `\nTotal: $${total.toFixed(2)}\n\n¿Puedes ayudarme a completar la compra?`;
          return mensaje;
        }

        // Cerrar modal de imagen
        document.getElementById("cerrarModal").addEventListener("click", () => {
          document.getElementById("modalImagen").style.display = "none";
        });

        cargarProductos();
      });