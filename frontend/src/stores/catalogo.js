import { defineStore } from 'pinia'
import { ref } from 'vue'
import categoriasService from 'src/services/categorias'
import usuariosService from 'src/services/usuarios'

/** Cadastros de apoio: categorias e usuarios. Usado pelos formularios e pelas telas de Admin. */
export const useCatalogoStore = defineStore('catalogo', () => {
  const categorias = ref([])
  const usuarios = ref([])
  const carregando = ref(false)

  async function carregarCategorias(incluirInativas = false) {
    carregando.value = true
    try {
      const { data } = await categoriasService.listar(incluirInativas)
      categorias.value = data
      return data
    } finally {
      carregando.value = false
    }
  }

  async function salvarCategoria(categoria) {
    const resposta = categoria.id
      ? await categoriasService.atualizar(categoria.id, categoria)
      : await categoriasService.criar(categoria)
    await carregarCategorias(true)
    return resposta.data
  }

  async function desativarCategoria(id) {
    await categoriasService.desativar(id)
    await carregarCategorias(true)
  }

  async function carregarUsuarios() {
    carregando.value = true
    try {
      const { data } = await usuariosService.listar()
      usuarios.value = data
      return data
    } finally {
      carregando.value = false
    }
  }

  async function salvarUsuario(usuario) {
    const resposta = usuario.id
      ? await usuariosService.atualizar(usuario.id, usuario)
      : await usuariosService.criar(usuario)
    await carregarUsuarios()
    return resposta.data
  }

  async function desativarUsuario(id) {
    await usuariosService.desativar(id)
    await carregarUsuarios()
  }

  return {
    categorias,
    usuarios,
    carregando,
    carregarCategorias,
    salvarCategoria,
    desativarCategoria,
    carregarUsuarios,
    salvarUsuario,
    desativarUsuario
  }
})
