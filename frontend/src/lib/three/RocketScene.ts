import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import type { PreviewMesh } from '../api/types.js';

/**
 * Three.js scene that renders a single rocket mesh fed from the server's
 * /api/designs/preview endpoint. We don't tessellate on the client — same
 * model lives on the server so the preview matches the eventual export
 * 1:1.
 */
export class RocketScene {
  private renderer: THREE.WebGLRenderer;
  private scene: THREE.Scene;
  private camera: THREE.PerspectiveCamera;
  private controls: OrbitControls;
  private rocket: THREE.Mesh | null = null;
  private wireframe: THREE.LineSegments | null = null;
  private raf: number | null = null;

  constructor(canvas: HTMLCanvasElement) {
    this.renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
    this.renderer.setPixelRatio(window.devicePixelRatio);
    const r = canvas.getBoundingClientRect();
    this.renderer.setSize(r.width, r.height, false);
    this.renderer.setClearColor(0x0e1116);

    this.scene = new THREE.Scene();
    this.camera = new THREE.PerspectiveCamera(45, r.width / r.height, 0.001, 100);
    this.camera.position.set(0.8, 0.5, 0.8);

    this.controls = new OrbitControls(this.camera, canvas);
    this.controls.enableDamping = true;

    const key = new THREE.DirectionalLight(0xffffff, 1.2);
    key.position.set(2, 3, 2);
    this.scene.add(key);
    const fill = new THREE.AmbientLight(0xaab4c2, 0.4);
    this.scene.add(fill);

    // X-axis indicator so users can see the rocket's nose direction.
    const axis = new THREE.AxesHelper(0.05);
    this.scene.add(axis);

    this.animate();
    window.addEventListener('resize', this.handleResize);
  }

  private handleResize = () => {
    const canvas = this.renderer.domElement;
    const r = canvas.getBoundingClientRect();
    this.camera.aspect = r.width / r.height;
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(r.width, r.height, false);
  };

  setMesh(mesh: PreviewMesh) {
    if (this.rocket) {
      this.scene.remove(this.rocket);
      this.rocket.geometry.dispose();
      (this.rocket.material as THREE.Material).dispose();
    }
    if (this.wireframe) {
      this.scene.remove(this.wireframe);
      this.wireframe.geometry.dispose();
      (this.wireframe.material as THREE.Material).dispose();
    }

    const geom = new THREE.BufferGeometry();
    geom.setAttribute('position', new THREE.Float32BufferAttribute(mesh.positions, 3));
    geom.setIndex(mesh.indices);
    geom.computeVertexNormals();
    // Rocket axis is +X in our world; Three.js cameras feel more natural
    // with +Y up. Rotate the mesh so the rocket stands vertical for display.
    geom.rotateZ(Math.PI / 2);
    geom.computeBoundingSphere();

    const mat = new THREE.MeshStandardMaterial({
      color: mesh.manifold ? 0xc7d1de : 0xff8866,
      metalness: 0.05,
      roughness: 0.55,
      flatShading: false
    });
    this.rocket = new THREE.Mesh(geom, mat);
    this.scene.add(this.rocket);

    // Frame the rocket.
    const bounds = geom.boundingSphere;
    if (bounds) {
      const d = bounds.radius * 3.0;
      this.camera.position.set(d, d * 0.6, d);
      this.controls.target.copy(bounds.center);
      this.controls.update();
    }
  }

  showWireframe(on: boolean) {
    if (!this.rocket) return;
    if (on && !this.wireframe) {
      const wireGeom = new THREE.WireframeGeometry(this.rocket.geometry);
      this.wireframe = new THREE.LineSegments(
        wireGeom,
        new THREE.LineBasicMaterial({ color: 0x4a90e2, opacity: 0.4, transparent: true })
      );
      this.scene.add(this.wireframe);
    } else if (!on && this.wireframe) {
      this.scene.remove(this.wireframe);
      this.wireframe.geometry.dispose();
      (this.wireframe.material as THREE.Material).dispose();
      this.wireframe = null;
    }
  }

  private animate = () => {
    this.raf = requestAnimationFrame(this.animate);
    this.controls.update();
    this.renderer.render(this.scene, this.camera);
  };

  dispose() {
    if (this.raf !== null) cancelAnimationFrame(this.raf);
    window.removeEventListener('resize', this.handleResize);
    this.renderer.dispose();
    this.controls.dispose();
  }
}
